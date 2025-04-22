package de.ngloader.referee.module.registry;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import de.ngloader.referee.Referee;
import de.ngloader.referee.RefereeConfig;
import de.ngloader.referee.RefereeLogger;
import de.ngloader.referee.command.RefereeCommand;
import de.ngloader.referee.module.RefereeModule;

public class DownloadAllModule extends RefereeModule {
	
	private final RefereeConfig config;
	
	private final Set<String> used = new HashSet<String>();

	private int folders = 0;
	private int files = 0;

	public DownloadAllModule(Referee app) {
		super(app, "DownloadAll");
		this.config = app.getConfig();
	}

	@Override
	protected void onInitalize() {
	}

	@Override
	protected void onEnable() {
		try {
			this.downloadAll();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDisable() {
	}

	@Override
	protected List<RefereeCommand> registerCommands() {
		return List.of();
	}

	void downloadAll() throws IOException, InterruptedException {
		HttpClient client = this.requestNewLogin();
		if (client == null) {
			throw new NullPointerException("Invalid login data for ilias!");
		}
		
		try {
			this.requestDownloadFolder(client, "", "https://meister.bfe-elearning.de/ilias.php?baseClass=ilrepositorygui&ref_id=1");
			System.out.println("Download completed! (Folders: " + this.folders + ", Files: " + this.files + ")");
		} finally {
			client.shutdownNow();
		}
	}
	
	private HttpClient requestNewLogin() throws IOException, InterruptedException {
		CookieManager cookieManager = new CookieManager();
		HttpClient client = HttpClient.newBuilder()
				.followRedirects(Redirect.NEVER)
				.cookieHandler(cookieManager)
				.build();

		HttpRequest getRequest = HttpRequest.newBuilder()
				.uri(URI.create("https://meister.bfe-elearning.de/ilias.php?baseClass=ilStartupGUI"))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.build();
		client.send(getRequest, HttpResponse.BodyHandlers.ofString());

		String boundary = "----geckoformboundary" + UUID.randomUUID().toString().replace("-", "");
		String CRLF = "\r\n";

		String body = "--" + boundary + CRLF +
				"Content-Disposition: form-data; name=\"login_form/input_3/input_4\"" + CRLF + CRLF +
				this.config.getIliasUsername() + CRLF +
				"--" + boundary + CRLF +
				"Content-Disposition: form-data; name=\"login_form/input_3/input_5\"" + CRLF + CRLF +
				this.config.getIliasPasswort() + CRLF +
				"--" + boundary + "--" + CRLF;

		HttpRequest postRequest = HttpRequest.newBuilder()
				.uri(URI.create("https://meister.bfe-elearning.de/ilias.php?baseClass=ilstartupgui&cmd=post&fallbackCmd=doStandardAuthentication"))
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString());

		return postResponse.statusCode() == 302 ? client : null;
	}

	private void requestDownloadFolder(HttpClient client, String currentFolder, String url) throws IOException, InterruptedException {
		this.folders++;
		
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "text/html; charset=UTF-8")
				.build();

		HttpResponse<Stream<String>> response = client.send(request, BodyHandlers.ofLines());
		List<String> body = response.body().toList();

		List<String[]> folderList = body.stream()
			.filter(line ->
				line.contains("<a href=") &&
//				line.contains("class=\"il_ContainerItemTitle\" target=\"_blank\"") &&
				line.contains("goto.php/"))
			.map(line -> {
				String fileName = line.substring(line.indexOf(">") + 1, line.length() - 4);
				
				int startIndex = line.indexOf("href=\"");
				line = line.substring(startIndex + 6);

				int endIndex = line.indexOf("\"");
				line = line.substring(0, endIndex);
				
				String[] folderId = line.split("/");
				String folderUrl = "https://meister.bfe-elearning.de/ilias.php?baseClass=ilrepositorygui&ref_id=" + folderId[folderId.length - 1];
				return new String[] { fileName, folderUrl };
			})
			.filter(checkFile -> this.used.add(checkFile[1]))
			.toList();

		
		List<String[]> fileList = new ArrayList<String[]>();
		String currentName = null;
		String currentUrl = null;
		boolean detectNext = false;
		for (String line : body) {			
			if (line.contains("<a href=") && line.contains("cmd=sendfile")) {
				detectNext = false;

				if (currentName != null && currentUrl != null) {
					fileList.add(new String[] { currentName + ".unknown", currentUrl });
					System.out.println("WARNING: Found file with unknown type! (" + currentFolder + "/" + currentName + ")");
				}
				
				currentName = line.substring(line.indexOf(">") + 1, line.length() - 4);
				
				int startIndex = line.indexOf("href=\"");
				currentUrl = line.substring(startIndex + 6);
	
				int endIndex = currentUrl.indexOf("\"");
				currentUrl = currentUrl.substring(0, endIndex);
				continue;
			}
			
			if (currentName != null && currentUrl != null && line.contains("ilListItemSection il_ItemProperties")) {
				detectNext = true;
				continue;
			}
			
			if (detectNext && line.endsWith("</span>")) {
				// \t\tpdf&nbsp;&nbsp;</span>
				String type = line.substring("\t\t".length(), line.length() - "</span>".length())
						.replaceAll("&nbsp;", "");
				fileList.add(new String[] { currentName + "." + type, currentUrl });

				detectNext = false;
				currentName = null;
				currentUrl = null;
				continue;
			}
		}
		
		if (currentName != null && currentUrl != null) {
			fileList.add(new String[] { currentName + ".unknown", currentUrl });
			System.out.println("WARNING: Found file with unknown type! (" + currentFolder + "/" + currentName + ")");
		}
		
		
//		List<String[]> fileList = body.stream()
//			.filter(line ->
//				line.contains("<a href=") &&
////				line.contains("class=\"il_ContainerItemTitle\" target=\"_blank\"") &&
//				line.contains("cmd=sendfile"))
//			.map(line -> {
//				String fileName = line.substring(line.indexOf(">") + 1, line.length() - 4);
//				
//				int startIndex = line.indexOf("href=\"");
//				line = line.substring(startIndex + 6);
//	
//				int endIndex = line.indexOf("\"");
//				line = line.substring(0, endIndex);
//				return new String[] { fileName, line };
//			})
//			.filter(checkFile -> this.used.add(checkFile[1]))
//			.toList();
		
		System.out.println("Found Inside folder '" + currentFolder + "' " + folderList.size() + " folders and " + fileList.size() + " files.");
		
		for (String[] file : fileList) {
			this.downloadFile(client, currentFolder, file[0], file[1]);
		}
		
		for (String[] folder : folderList) {
			this.requestDownloadFolder(client, currentFolder + "/" + folder[0], folder[1]);
		}
	}

	private Path downloadFile(HttpClient client, String path, String name, String downloadLink) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(downloadLink))
				.GET()
				.header("User-Agent", "NgLoader/1.0.0")
				.header("Cache-Control", "no-cache")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.build();

		Files.createDirectories(Path.of("./data/download/", path));
		
		System.out.println("Downloading: " + path + "/" + name);

		this.files++;
		HttpResponse<Path> response = client.send(request, BodyHandlers.ofFile(Path.of("./data/download", path, name)));
		return response.body();
	}

	public void destroy() {
		RefereeLogger.info("Schoolplan destroyed...");
	}
	
	public String getAdminRoleId() {
		return this.config.getAdminRoleId().asString();
	}
}