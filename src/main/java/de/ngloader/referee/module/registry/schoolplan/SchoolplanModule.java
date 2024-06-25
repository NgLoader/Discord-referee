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
import java.util.concurrent.atomic.AtomicBoolean;
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

	private TextChannel channel;

	private String previousFile;
	private Color previousColor;

	private Thread currentThread;
	private AtomicBoolean running = new AtomicBoolean(false);
	private int failedRequestCount = 0;

	public SchoolplanModule(Referee app) {
		super(app, "Schoolplan");
		this.config = app.getConfig();
	}

	@Override
	protected void onInitalize() {
		this.channel = (TextChannel) app.getGuild().getChannelById(config.getPlanChannelId()).block();
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
		boolean isRunning = this.running.get();
		this.stopScheduler();

		try {
			this.updatePlan(force);
		} catch (IOException | InterruptedException e) {
			RefereeLogger.error("Failed to update plan!", e);
		}

		if (isRunning) {
			this.startScheduler();
		}
	}

	public boolean startScheduler() {
		if (this.running.compareAndSet(false, true)) {
			this.currentThread = new Thread(() -> this.tickTask(), "Referee - Schoolplan - Scheduler");
			this.currentThread.start();
			
			RefereeLogger.info("Schoolplan scheduler started.");
			return true;
		}
		return false;
	}

	public boolean stopScheduler() {
		boolean runningToggled = this.running.compareAndSet(true, false);
		if (this.currentThread != null) {
			if (!this.currentThread.isInterrupted()) {
				this.currentThread.interrupt();
			}
			
			this.currentThread = null;

			RefereeLogger.info("Schoolplan scheduler stopped.");
			return runningToggled;
		}
		return false;
	}

	private void tickTask() {
		while(this.running.get()) {
			try {
				this.updatePlan(false);
			} catch (Exception e) {
				RefereeLogger.error(String.format("Something went wrong! Try (%d/30)", this.failedRequestCount++), e);
				
				if (this.failedRequestCount > 30) {
					RefereeLogger.warn("Stopping tick task from plan scheduler because alle 30 request failed!");
					this.stopScheduler();
					
					this.channel.createMessage(String.format("<@%s> Stundenplan scheduler wegen störungen pausiert! Bitte logs überprüfen!",
							this.config.getAdminRoleId().asString())
						).subscribe();
					return;
				}
				
				RefereeLogger.info("Request will retry in 30min!");
			}
			
			try {
				Thread.sleep(1000 * 60 * 30); // check for updates every 30min.
			} catch (InterruptedException e) {
				// ignore interrupted exception
			} catch (Exception e) {
				RefereeLogger.error("Error in tick task from plan scheduler! stopping task", e);
				this.stopScheduler();
			}
		}
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

	private void updatePlan(boolean force) throws IOException, InterruptedException {
		try (HttpClient client = this.requestNewLogin()) {
			if (client == null) {
				throw new NullPointerException("Invalid login data for ilias!");
			}
			
			String download = this.requestLatestDownloadPath(client);
			if (!force && this.previousFile != null && this.previousFile.equalsIgnoreCase(download)) {
				return;
			}
			this.previousFile = download;

			Path path = this.downloadFile(client, download);
			// TODO invalidate login after download
			
			List<ScheduleWeekly> scheduleList = this.analyzePlan(path);
			
			if (scheduleList != null) {
				Optional<ScheduleWeekly> scheduleOptional = scheduleList.stream().filter(schedule -> schedule.course().endsWith("HVI 24/1")).findAny();
				if (scheduleOptional.isEmpty()) {
					return;
				}

				this.printPlan(scheduleOptional.get());
				this.savePrevious();
			}
		}
	}
	
	private void printPlan(ScheduleWeekly schedule) {
		List<ScheduleDaily> dailySchedule = schedule.daily();

		Color color;
		Random random = new Random();
		do {
			color = COLOR_LIST[random.nextInt(COLOR_LIST.length)];
		} while (this.previousColor == color);
		this.previousColor = color;

		Message headerMessage = this.channel.createMessage(MessageCreateSpec.builder()
				.content(String.format("<@%s> Es gibt einen neuen Stundenplan!", this.config.getPlanTagId()))
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
							this.channel.getGuildId().asString(),
							this.channel.getId().asString(),
							headerMessage.getId().asString()))
					.footer(schedule.date(), "");

			// increment for next day
			dayIndex++;

			int lessonIndex = 0;
			for (ScheduleLesson abstractLesson : daily.lessons()) {
				if (abstractLesson instanceof ScheduleLessonEntry lesson) {
					embedBuilder.addField(
							String.format("**%s** ``Raum: %s``", DAY_HOUR_LIST[lessonIndex], lesson.room()),
							String.format("**%s** ``(%s)``\n"
									+ "**%s** ``%s``\n%s",
									NameMapper.COURSE.getName(lesson.course()),
									lesson.course(),
									NameMapper.TEACHER.getName(lesson.teacher()),
									lesson.teacher(),
									lesson.addition() != null && !lesson.addition().isBlank()
										? "**Info: ** ``" + lesson.addition() + "``"
										: lesson.addition()),
							false);
				} else {
					embedBuilder.addField(
							String.format("**%s**", DAY_HOUR_LIST[lessonIndex]),
							String.format("**Info** ``%s``", abstractLesson.addition()),
							false);
				}
				
				lessonIndex++;
			}

			this.channel.createMessage(embedBuilder.build()).subscribe();
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
		this.stopScheduler();
	}
}