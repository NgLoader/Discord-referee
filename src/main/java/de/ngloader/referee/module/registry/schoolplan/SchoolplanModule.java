package de.ngloader.referee.module.registry.schoolplan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import de.ngloader.referee.Referee;
import de.ngloader.referee.RefereeConfig;
import de.ngloader.referee.RefereeLogger;
import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.module.RefereeModule;
import de.ngloader.referee.util.NameMapper;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;

public class SchoolplanModule extends RefereeModule {
	
	private static final Pattern PATTERN_LESSON = Pattern.compile(" ([A-Za-z]{4})([A-Za-z0-9-]+)\\s+([A-Za-z0-9]+)\\s+([A-Za-z0-9 .,]{0,}|\\s+)(Mo|Di|Mi|Do|Fr|Sa|So|[0-9])");
	private static final Pattern PATTERN_CUSTOM = Pattern.compile(" ([A-Za-z0-9]{1,})\\s{0,}(Mo|Di|Mi|Do|Fr|Sa|So|[0-9])");

	private static final Path PREVIOUS_FILE = Path.of("./data/cache/studenplan.previous.properties");

	private static final Color[] COLOR_LIST = new Color[] {
			Color.RED,
			Color.GREEN,
			Color.CYAN,
			Color.MAGENTA,
			Color.YELLOW,
			Color.VIVID_VIOLET,
			Color.BISMARK
	};

	private static final String[] DAY_NAME_LIST = new String[] {
			"Montag",
			"Dienstag",
			"Mittwoch",
			"Donnerstag",
			"Freitag",
			"Samstag",
			"Sonntag"
	};

	private static final String[] DAY_HOUR_LIST = new String[] {
			"07:30 - 09:00",
			"09:30 - 11:00",
			"11:30 - 13:00",
			"13:30 - 15:00",
			"missing - missing",
			"missing - missing",
			"missing - missing",
			"missing - missing"
	};

	private final RefereeConfig config;

	private final List<ScheduleClass> classes = new ArrayList<>();

	private SchoolplanThread updateTask;
	
	private String previousFile;
	private Color previousColor;

	public SchoolplanModule(Referee app) {
		super(app, "Schoolplan");
		this.config = app.getConfig();
	}

	@Override
	protected void onInitalize() {
		Properties properties = this.config.getProperties();
		
		int index = 0;
		for (;;) {
			String name = properties.getProperty(String.format("schoolplan.%d.name", index), null);
			String tagId = properties.getProperty(String.format("schoolplan.%d.tagId", index), null);
			String channelId = properties.getProperty(String.format("schoolplan.%d.channelId", index), null);
			
			if (name == null && tagId == null && channelId == null) {
				break;
			} else if (name == null) {
				RefereeLogger.warn("Schoolplan: Missing name by index " + index);
				continue;
			} else if (tagId == null) {
				RefereeLogger.warn("Schoolplan: Missing tagId by index " + index);
				continue;
			} else if (channelId == null) {
				RefereeLogger.warn("Schoolplan: Missing channelId by index " + index);
				continue;
			}
			
			Snowflake snowflakeChannelId = Snowflake.of(channelId);
			TextChannel textChannel = (TextChannel) app.getGuild().getChannelById(snowflakeChannelId).block();
			
			this.classes.add(new ScheduleClass(name, tagId, textChannel));
			index++;
		}
		
		RefereeLogger.info("Schoolplan: Loaded " + this.classes.size() + " classes.");
	}

	@Override
	protected void onEnable() {
		this.loadPrevious();
		this.startScheduler();
	}

	@Override
	protected void onDisable() {
		this.stopScheduler();
	}

	@Override
	protected List<RefereeCommand> registerCommands() {
		return List.of(new SchoolplanCommand(this));
	}

	public void forceUpdate(boolean force) {
		if (this.updateTask != null) {
			this.updateTask.forceUpdate();
			return;
		}

		try {
			this.updatePlan(force);
		} catch (IOException | InterruptedException e) {
			RefereeLogger.error("Failed to update plan!", e);
		}
	}

	public boolean startScheduler() {
		if (this.updateTask != null) {
			return false;
		}
		
		this.updateTask = new SchoolplanThread(this);
		this.updateTask.start();

		RefereeLogger.info("Schoolplan scheduler started.");
		return true;
	}

	public boolean stopScheduler() {
		if (this.updateTask == null) {
			return false;
		}
		
		this.updateTask.destroy();
		this.updateTask = null;

		RefereeLogger.info("Schoolplan scheduler stopped.");
		return true;
	}

	private void savePrevious() {
		try {
			Files.createDirectories(PREVIOUS_FILE.getParent());

			Properties properties = new Properties();
			properties.setProperty("file", this.previousFile);
			properties.setProperty("color", String.valueOf(this.previousColor.getRGB()));

			try (OutputStream outputStream = Files.newOutputStream(PREVIOUS_FILE)) {
				properties.store(outputStream, Instant.now().toString());
			}
		} catch (IOException e) {
			RefereeLogger.error("Error by loading previous file", e);
		}
	}

	private void loadPrevious() {
		try {
			if (Files.notExists(PREVIOUS_FILE)) {
				return;
			}

			Properties properties = new Properties();
			try (InputStream inputStream = Files.newInputStream(PREVIOUS_FILE)) {
				properties.load(inputStream);
			}

			this.previousFile = properties.getProperty("file", null);
			String color = properties.getProperty("color", null);
			this.previousColor = color != null ? Color.of(Integer.valueOf(color)) : null;
		} catch (IOException e) {
			RefereeLogger.error("Error by loading previous file", e);
		}
	}

	void updatePlan(boolean force) throws IOException, InterruptedException {
		HttpClient client = this.requestNewLogin();
		if (client == null) {
			throw new NullPointerException("Invalid login data for ilias!");
		}
		
		try {
			String download = this.requestLatestDownloadPath(client);
			if (!force && this.previousFile != null && this.previousFile.equalsIgnoreCase(download)) {
				return;
			}
			this.previousFile = download;

			Path path = this.downloadFile(client, download);
			// TODO invalidate login after download
			
			List<ScheduleWeekly> scheduleList = this.analyzePlan(path);
			
			if (scheduleList != null) {
				// update display color
				Color color;
				Random random = new Random();
				do {
					color = COLOR_LIST[random.nextInt(COLOR_LIST.length)];
				} while (this.previousColor == color);
				this.previousColor = color;
				
				// print new plan
				for (ScheduleClass clazz : this.classes) {
					Optional<ScheduleWeekly> scheduleOptional = scheduleList.stream().filter(schedule -> schedule.course().endsWith(clazz.name())).findAny();
					if (scheduleOptional.isEmpty()) {
						RefereeLogger.warn("Unable to find name in scheduler \'" + clazz.name() + "\'");
						return;
					}

					this.printPlan(scheduleOptional.get(), clazz);
				}
				
				// save changes
				this.savePrevious();
			}
		} finally {
			client.shutdownNow();
		}
	}
	
	private void printPlan(ScheduleWeekly schedule, ScheduleClass clazz) {
		List<ScheduleDaily> dailySchedule = schedule.daily();

		Message headerMessage = clazz.channel().createMessage(MessageCreateSpec.builder()
				.content(String.format("<@%s> Es gibt einen neuen Stundenplan!", clazz.tagId()))
				.addEmbed(EmbedCreateSpec.builder()
						.title(schedule.date())
						.footer(String.format("Kurs: %s\nKlassenlehrer: %s",
								schedule.course(),
								schedule.teacher()),
							"")
						.url(this.previousFile)
						.color(this.previousColor)
						.build())
				.build())
		.block();

		int dayIndex = 0;
		for (ScheduleDaily daily : dailySchedule) {
			EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
					.title(DAY_NAME_LIST[dayIndex])
					.color(this.previousColor)
					.description(String.format("[Übersicht](https://discordapp.com/channels/%s/%s/%s)",
							clazz.channel().getGuildId().asString(),
							clazz.channel().getId().asString(),
							headerMessage.getId().asString()))
					.footer(schedule.date(), "");

			// increment for next day
			dayIndex++;

			int lessonIndex = 0;
			for (ScheduleLesson abstractLesson : daily.lessons()) {
				String addition = NameMapper.ADDITION.getName(abstractLesson.addition());
				boolean hasAddition = !addition.isBlank();
				
				if (abstractLesson instanceof ScheduleLessonEntry lesson) {
					embedBuilder.addField(
							String.format("**%s** ``Raum: %s``", DAY_HOUR_LIST[lessonIndex], lesson.room()),
							String.format("**%s** ``(%s)``\n"
									+ "**%s** ``%s``\n%s",
									NameMapper.COURSE.getName(lesson.course()),
									lesson.course(),
									NameMapper.TEACHER.getName(lesson.teacher()),
									lesson.teacher(),
									hasAddition ? "**Info: ** ``" + addition + "``" : ""),
							false);
				} else {
					embedBuilder.addField(
							String.format("**%s**", DAY_HOUR_LIST[lessonIndex]),
							String.format("**Info** ``%s``", addition),
							false);
				}
				
				lessonIndex++;
			}

			clazz.channel().createMessage(embedBuilder.build()).subscribe();
		}
	}
	
	private List<ScheduleWeekly> analyzePlan(Path path) throws IOException {
        List<ScheduleWeekly> lehrgangs = new ArrayList<>();

		try (PDDocument document = PDDocument.load(path.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			String[] lines = text.split("\\r?\\n");

			String date = null;

			String course = null;
			String currentTeacher = null;
			List<ScheduleLesson> dailySchedule = new ArrayList<>();
			List<ScheduleDaily> weeklySchedule = new ArrayList<>();

			for (String line : lines) {
            	if (line.contains("Stundenplan") && line.contains("vom") && line.contains("bis")) {
            		date = line.trim();
				} else if (line.matches(".*\\bMeister-Lgg\\b.*")) {
					if (course != null) {
						if (!dailySchedule.isEmpty()) {
							weeklySchedule.add(new ScheduleDaily(new ArrayList<>(dailySchedule)));
							dailySchedule.clear();
						}
						lehrgangs.add(new ScheduleWeekly(date, course, currentTeacher, new ArrayList<>(weeklySchedule)));
						weeklySchedule.clear();
					}
					course = line.trim();
					currentTeacher = null;
				} else if (course != null && currentTeacher == null) {
					currentTeacher = line.trim();
				} else {
					// skip parsing for invalid type
					if (line.startsWith("Kostenträger")) {
						continue;
					}
					
					if (line.matches(".*\\s+([A-Za-z]{0,}(Mo|Di|Mi|Do|Fr|Sa|So))")) {
						if (!dailySchedule.isEmpty()) {
							weeklySchedule.add(new ScheduleDaily(new ArrayList<>(dailySchedule)));
							dailySchedule.clear();
						}
					}
					
					Matcher matcherLesson = PATTERN_LESSON.matcher(line);						/*
					 * pattern:
					 * <Teacher>-<Type> <Room> <Room>
					 * <Teacher>-<Type> <Room> <Addition> <Room>
					 * <Teacher>-<Type> <Room> <Addition><Room>
					 *  
					 *  group:
					 *  1: teacher
					 *  2: type
					 *  3: room
					 *  4: addition
					 *  5: room
					 * 
					 * example valid:
					 *  NIGEde-v 404 contentFr
					 *  NIGEde-v 404 content Fr
					 *  NIGEde-v 404 Fr
					 *  NIGEde-v 404 content3
					 *  NIGEde-v 404 content 3
					 *  NIGEde-v 404 3
					 *  NIGEdev 404 content3
					 *  NIGEdev 404 content 3
					 */
					if (matcherLesson.find()) {
						String teacher = matcherLesson.group(1);
						String type = matcherLesson.group(2);
						String room = matcherLesson.group(3);
						String addition = matcherLesson.group(4);
						String schedule = matcherLesson.group(5);
						int scheduleInt = schedule.matches("[0-9]+") ? Integer.valueOf(schedule) : 0;

						dailySchedule.add(new ScheduleLessonEntry(teacher, type, room, addition, scheduleInt));
						continue;
					}
					
					Matcher matcherCustom = PATTERN_CUSTOM.matcher(line);						/*
					 * pattern:
					 * <Addition> <Room>
					 * <Addition><Room>
					 *  
					 *  group:
					 *  4: addition
					 *  5: room
					 * 
					 * example valid:
					 *  ufrei Fr
					 *  ufrei Frz
					 *  ufrei 1
					 *  ufrei 1z
					 */
					if (matcherCustom.find()) {
						String addition = matcherCustom.group(1);
						String schedule = matcherCustom.group(2);
						int scheduleInt = schedule.matches("[0-9]+") ? Integer.valueOf(schedule) : 0;
						
						dailySchedule.add(new ScheduleLessonCustom(addition, scheduleInt));
						continue;
					}
				}
			}

			if (course != null) {
				if (!dailySchedule.isEmpty()) {
					weeklySchedule.add(new ScheduleDaily(new ArrayList<>(dailySchedule)));
				}
				lehrgangs.add(new ScheduleWeekly(date, course, currentTeacher, new ArrayList<>(weeklySchedule)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        return lehrgangs;
	}

	private HttpClient requestNewLogin() throws IOException, InterruptedException {
		CookieManager cookieManager = new CookieManager();
		HttpClient client = HttpClient.newBuilder()
				.followRedirects(Redirect.NEVER)
				.cookieHandler(cookieManager)
				.build();

		Map<String, String> parameters = new HashMap<>();
		parameters.put("username", this.config.getIliasUsername());
		parameters.put("password", this.config.getIliasPasswort());
		parameters.put("cmd[doStandardAuthentication]", "Anmelden");

		String form = parameters.entrySet()
		    .stream()
		    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
		    .collect(Collectors.joining("&"));

		HttpRequest request = HttpRequest.newBuilder(URI.create("https://meister.bfe-elearning.de/ilias.php?client_id=c-il-meister&cmd=post&cmdClass=ilstartupgui&cmdNode=10k&baseClass=ilStartUpGUI"))
				.POST(BodyPublishers.ofString(form))
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return response.statusCode() == 302 ? client : null;
	}

	private String requestLatestDownloadPath(HttpClient client) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://meister.bfe-elearning.de/ilias.php?ref_id=43215&cmd=render&cmdClass=ilrepositorygui&cmdNode=xb&baseClass=ilrepositorygui"))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.build();

		HttpResponse<Stream<String>> response = client.send(request, BodyHandlers.ofLines());
		Optional<String> planList = response.body().filter(line -> line.contains("<a href=") && line.contains("Stundenplan KW")).findFirst();
		if (planList.isEmpty()) {
			return null;
		}
		String plan = planList.get();
		int startIndex = plan.indexOf("href=\"");
		plan = plan.substring(startIndex + 6);

		int endIndex = plan.indexOf("\"");
		plan = plan.substring(0, endIndex);
		return plan;
	}

	private Path downloadFile(HttpClient client, String downloadLink) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(downloadLink))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.build();

		Files.createDirectories(Path.of("./data/cache"));

		HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(Path.of("./data/cache/stundenplan.pdf")));
		return response.body();
	}

	public void destroy() {
		if (this.updateTask != null) {
			this.updateTask.destroy();
			this.updateTask = null;
		}
		
		this.stopScheduler();
		RefereeLogger.info("Schoolplan destroyed...");
	}
	
	public String getAdminRoleId() {
		return this.config.getAdminRoleId().asString();
	}
	
	public List<ScheduleClass> getClasses() {
		return this.classes;
	}
}