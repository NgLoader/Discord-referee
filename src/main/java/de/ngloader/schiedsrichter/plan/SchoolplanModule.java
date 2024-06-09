package de.ngloader.schiedsrichter.plan;

import java.io.IOException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import de.ngloader.schiedsrichter.Schiedsrichter;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class SchoolplanModule {

	private static final String LOGIN_USERNAME = ""; // TODO config
	private static final String LOGIN_PASSWORT = ""; // TODO config
	
	private TextChannel channel;

	public SchoolplanModule(Schiedsrichter app) {
		this.channel = (TextChannel) app.getGuild().getChannelById(Snowflake.of("1248303417298911303")).block();
		this.updatePlan();
	}
	
	public void updatePlan() {
		List<WeeklySchedule> scheduleList = null;
		try {
			HttpClient client = this.requestNewLogin();
			String download = this.requestLatestDownloadPath(client);
			Path path = this.downloadFile(client, download);
			scheduleList = this.analyzePlan(path);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		if (scheduleList != null) {
			Optional<WeeklySchedule> scheduleOptional = scheduleList.stream().filter(schedule -> schedule.course().endsWith("HVI 24/1")).findAny();
			if (scheduleOptional.isEmpty()) {
				return;
			}
			WeeklySchedule schedule = scheduleOptional.get();
			List<DailySchedule> dailySchedule = schedule.daily();
			
			List<String> test = new ArrayList<>();
			
			for (DailySchedule daily : dailySchedule) {
				StringBuilder builder = new StringBuilder();
				for (RoomInfo room : daily.lessons()) {
					builder.append(String.format("%d: **%s** bei ``%s``", room.index(), room.course(), room.teacher()));
					builder.append(System.lineSeparator());
				}
				test.add(builder.toString());
			}
			
			this.channel.createMessage(EmbedCreateSpec.builder()
					.title(schedule.date())
					.color(Color.CYAN)
					.addField("Montag", test.get(0), true)
					.addField("Dienstag", test.get(1), true)
					.addField("Mittwoch", test.get(2), true)
					.addField("Donnerstag", test.get(3), true)
					.addField("Freitag", test.get(4), true)
					.footer(String.format("Kurs: %s\nLehrer: %s", schedule.course(), schedule.teacher()), "")
					.timestamp(Instant.now())
					.build()).subscribe();
			this.channel.createMessage("<@&1248411558237962431> Es gibt einen neuen Stundenplan").subscribe();
		}
	}
	
	public List<WeeklySchedule> analyzePlan(Path path) throws IOException {
        List<WeeklySchedule> lehrgangs = new ArrayList<>();

		try (PDDocument document = PDDocument.load(path.toFile())) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(document);
			String[] lines = text.split("\\r?\\n");

			String date = null;
			
			String course = null;
			String currentTeacher = null;
			List<RoomInfo> dailySchedule = new ArrayList<>();
			List<DailySchedule> weeklySchedule = new ArrayList<>();

			for (String line : lines) {
            	if (line.contains("Stundenplan") && line.contains("vom") && line.contains("bis")) {
            		date = line.trim();
				} else if (line.matches(".*\\bMeister-Lgg\\b.*")) {
					if (course != null) {
						if (!dailySchedule.isEmpty()) {
							weeklySchedule.add(new DailySchedule(new ArrayList<>(dailySchedule)));
							dailySchedule.clear();
						}
						lehrgangs.add(new WeeklySchedule(date, course, currentTeacher, new ArrayList<>(weeklySchedule)));
						weeklySchedule.clear();
					}
					course = line.trim();
					currentTeacher = null;
				} else if (course != null && currentTeacher == null) {
					currentTeacher = line.trim();
				} else if (course != null && line.matches(" ([A-Za-z]+)-([A-Za-z])\\s([0-9]+)\\s+(Mo|Di|Mi|Do|Fr|Sa|So|[0-9])")) {
					String[] lesson = line.trim().split(" ");
					int schedule = 1;

					if (line.matches(".*\\s+(Mo|Di|Mi|Do|Fr|Sa|So)")) {
						if (!dailySchedule.isEmpty()) {
							weeklySchedule.add(new DailySchedule(new ArrayList<>(dailySchedule)));
							dailySchedule.clear();
						}
					} else {
						schedule = Integer.valueOf(lesson[lesson.length - 1]);
					}
					
					String techerWithType = lesson[0];
					String teacher = techerWithType.substring(0, 4);
					String type = techerWithType.substring(4);
					
					dailySchedule.add(new RoomInfo(teacher, type, lesson[1], schedule));
				}
			}
			
			if (course != null) {
				if (!dailySchedule.isEmpty()) {
					weeklySchedule.add(new DailySchedule(new ArrayList<>(dailySchedule)));
				}
				lehrgangs.add(new WeeklySchedule(date, course, currentTeacher, new ArrayList<>(weeklySchedule)));
			}
		}

        return lehrgangs;
	}
	
	public HttpClient requestNewLogin() throws IOException, InterruptedException {
		CookieManager cookieManager = new CookieManager();
		HttpClient client = HttpClient.newBuilder()
				.followRedirects(Redirect.NEVER)
				.cookieHandler(cookieManager)
				.build();
		
		Map<String, String> parameters = new HashMap<>();
		parameters.put("username", LOGIN_USERNAME);
		parameters.put("password", LOGIN_PASSWORT);
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
	
	public String requestLatestDownloadPath(HttpClient client) throws IOException, InterruptedException {
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
	
	public Path downloadFile(HttpClient client, String downloadLink) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(downloadLink))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.build();
		
		Files.createDirectories(Path.of("./data"));
		
		HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(Path.of("./data/stundenplan.pdf")));
		return response.body();
	}
}