package de.ngloader.referee.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import de.ngloader.referee.RefereeLogger;

public class NameMapper {

	public static final NameMapper COURSE = new NameMapper(Path.of("./data/courseNames.properties"));
	public static final NameMapper TEACHER = new NameMapper(Path.of("./data/teacherNames.properties"));

	public static void initalizeStaticFields() { }
	
	public static void reloadAll() {
		COURSE.reload();
		TEACHER.reload();
	}

	private final Path file;
	private final Map<String, String> nameMapping = new HashMap<>();
	
	private NameMapper(Path file) {
		this.file = file;

		synchronized (this.nameMapping) {
			this.load();
		}
	}
	
	private void load() {
		if (Files.notExists(this.file)) {
			RefereeLogger.warn("Missing " + this.file.getFileName().toString() + " file! Creating default file...");
			save();
		}
		
		Properties properties = new Properties();
		try (BufferedReader bufferedReader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
			properties.load(bufferedReader);
		} catch (IOException e) {
			RefereeLogger.error("Error by loading default " + this.file.getFileName().toString() + " file!", e);
		}
		
		for (Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<Object, Object> entry = iterator.next();
			this.nameMapping.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
		}
	}
	
	private void save() {
		Properties properties = new Properties();
		properties.setProperty("nilsg", "Nils Gereke");
		properties.setProperty("dev", "Developer");
		
		try {
			Files.createDirectories(this.file.getParent());
			
			try (BufferedWriter bufferedWriter = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8)) {
				properties.store(bufferedWriter, Instant.now().toString());
			}
		} catch (Exception e) {
			RefereeLogger.error("Error by creating default " + this.file.getFileName().toString() + " file!", e);
		}
	}
	
	public void reload() {
		synchronized (this.nameMapping) {
			this.nameMapping.clear();
			this.load();
		}
	}
	
	public String getName(String name) {
		synchronized (this.nameMapping) {
			return name != null ? nameMapping.getOrDefault(name.toLowerCase(), name) : name;
		}
	}
}
