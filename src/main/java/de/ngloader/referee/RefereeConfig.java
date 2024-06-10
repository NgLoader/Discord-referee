package de.ngloader.referee;

<<<<<<< feat-purge-command
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
=======
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
>>>>>>> master
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;

import discord4j.common.util.Snowflake;

public class RefereeConfig {
	
	private static final Path CONFIG_FILE = Path.of("./data/config.properties");
	
	private final Properties properties = new Properties();
	
	private String botToken = "your bot token";
	
	private String iliasUsername = "login username";
	private String iliasPasswort = "login passwort";
	
	private Snowflake guildId = Snowflake.of(0);
<<<<<<< feat-purge-command
	private Snowflake adminRoleId = Snowflake.of(0);
	
=======
>>>>>>> master
	private Snowflake planChannelId = Snowflake.of(0);
	private String planTagId = "128286216708685824";
	
	RefereeConfig() { }
	
	public boolean loadConfig() {
		if (Files.notExists(CONFIG_FILE)) {
			RefereeLogger.warn("Missing config file! Creating default config...");
			this.saveConfig();
			return false;
		}
		
		this.properties.clear();
		
<<<<<<< feat-purge-command
		try (BufferedReader bufferedReader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
			this.properties.load(bufferedReader);
=======
		try (InputStream inputStream = Files.newInputStream(CONFIG_FILE)) {
			this.properties.load(inputStream);
>>>>>>> master
		} catch (IOException e) {
			RefereeLogger.error("Error by loading config file!", e);
			return false;
		}
		
		this.botToken = this.properties.getProperty("botToken");
<<<<<<< feat-purge-command
		this.guildId = Snowflake.of(this.properties.getProperty("guildId", "0"));
		this.adminRoleId = Snowflake.of(this.properties.getProperty("adminRoleId", "0"));

		this.iliasUsername = this.properties.getProperty("iliasUsername");
		this.iliasPasswort = this.properties.getProperty("iliasPasswort");
		
		this.planChannelId = Snowflake.of(this.properties.getProperty("planChannelId", "0"));
=======

		this.iliasUsername = this.properties.getProperty("iliasUsername");
		this.iliasPasswort = this.properties.getProperty("iliasPasswort");

		this.guildId = Snowflake.of(this.properties.getProperty("guildId"));
		this.planChannelId = Snowflake.of(this.properties.getProperty("planChannelId"));
>>>>>>> master
		this.planTagId = this.properties.getProperty("planTagId");
		
		return true;
	}
	
	public void saveConfig() {
		try {
			Files.createDirectories(CONFIG_FILE.getParent());
		} catch (IOException e) {
			RefereeLogger.error("Error by creating directories!", e);
			return;
		}
		
		this.properties.setProperty("botToken", this.botToken);
<<<<<<< feat-purge-command
		this.properties.setProperty("guildId", this.guildId.asString());
		this.properties.setProperty("adminRoleId", this.adminRoleId.asString());
=======
>>>>>>> master

		this.properties.setProperty("iliasUsername", this.iliasUsername);
		this.properties.setProperty("iliasPasswort", this.iliasPasswort);

<<<<<<< feat-purge-command
		this.properties.setProperty("planChannelId", this.planChannelId.asString());
		this.properties.setProperty("planTagId", this.planTagId);
		
		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
			this.properties.store(bufferedWriter, new Date().toInstant().toString());
=======
		this.properties.setProperty("guildId", this.guildId.asString());
		this.properties.setProperty("planChannelId", this.planChannelId.asString());
		this.properties.setProperty("planTagId", this.planTagId);
		
		try (OutputStream outputStream = Files.newOutputStream(CONFIG_FILE)) {
			this.properties.store(outputStream, new Date().toInstant().toString());
>>>>>>> master
		} catch (IOException e) {
			RefereeLogger.error("Error by saving config file!", e);
		}
	}
	
	public String getBotToken() {
		return this.botToken;
	}
	
<<<<<<< feat-purge-command
	public Snowflake getGuildId() {
		return this.guildId;
	}
	
	public Snowflake getAdminRoleId() {
		return this.adminRoleId;
	}
	
=======
>>>>>>> master
	public String getIliasUsername() {
		return this.iliasUsername;
	}
	
	public String getIliasPasswort() {
		return this.iliasPasswort;
	}
	
<<<<<<< feat-purge-command
=======
	public Snowflake getGuildId() {
		return this.guildId;
	}
	
>>>>>>> master
	public Snowflake getPlanChannelId() {
		return this.planChannelId;
	}
	
	public String getPlanTagId() {
		return this.planTagId;
	}
}