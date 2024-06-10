package de.ngloader.referee.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import de.ngloader.referee.RefereeLogger;

public class TeacherName {

	private static final Path FILE_PATH = Path.of("./data/teacherNames.properties");
	
	private static final Map<String, String> NAME_MAPPING = new HashMap<>();
	
	static {
		if (Files.notExists(FILE_PATH)) {
			RefereeLogger.warn("Missing teacherNames.properties file! Creating default file...");
			createDefaultFile();
		}
		
		Properties properties = new Properties();
		try (InputStream inputStream = Files.newInputStream(FILE_PATH)) {
			properties.load(inputStream);
		} catch (IOException e) {
			RefereeLogger.error("Error by loading default teacherNames.properties file!", e);
		}
		
		for (Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<Object, Object> entry = iterator.next();
			NAME_MAPPING.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
		}
	}
	
	private static void createDefaultFile() {
		Properties properties = new Properties();
		properties.setProperty("nilsg", "Nils Gereke");
		
		try {
			Files.createDirectories(FILE_PATH.getParent());
			
			try (OutputStream outputStream = Files.newOutputStream(FILE_PATH)) {
				properties.store(outputStream, Instant.now().toString());
			}
		} catch (Exception e) {
			RefereeLogger.error("Error by creating default teacherNames.properties file!", e);
		}
	}
	
	public static String getName(String name) {
		return name != null ? NAME_MAPPING.getOrDefault(name.toLowerCase(), name) : name;
	}
	
	public static void initalizeStaticFields() { }

	private TeacherName() { }
}
