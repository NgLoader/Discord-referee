package de.ngloader.referee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	private Snowflake planChannelId = Snowflake.of(0);
	private Snowflake planTagId = Snowflake.of(0);
	
	RefereeConfig() { }
	
	public boolean loadConfig() {
		if (Files.notExists(CONFIG_FILE)) {
			RefereeLogger.warn("Missing config file! Creating default config...");
			this.saveConfig();
			return false;
		}
		
		this.properties.clear();
		
		try (InputStream inputStream = Files.newInputStream(CONFIG_FILE)) {
			this.properties.load(inputStream);
		} catch (IOException e) {
			RefereeLogger.error("Error by loading config file!", e);
			return false;
		}
		
		this.botToken = this.properties.getProperty("botToken");

		this.iliasUsername = this.properties.getProperty("iliasUsername");
		this.iliasPasswort = this.properties.getProperty("iliasPasswort");

		this.guildId = Snowflake.of(this.properties.getProperty("guildId"));
		this.planChannelId = Snowflake.of(this.properties.getProperty("planChannelId"));
		this.planTagId = Snowflake.of(this.properties.getProperty("planTagId"));
		
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

		this.properties.setProperty("iliasUsername", this.iliasUsername);
		this.properties.setProperty("iliasPasswort", this.iliasPasswort);

		this.properties.setProperty("guildId", this.guildId.asString());
		this.properties.setProperty("planChannelId", this.planChannelId.asString());
		this.properties.setProperty("planTagId", this.planTagId.asString());
		
		try (OutputStream outputStream = Files.newOutputStream(CONFIG_FILE)) {
			this.properties.store(outputStream, new Date().toInstant().toString());
		} catch (IOException e) {
			RefereeLogger.error("Error by saving config file!", e);
		}
	}
	
	public String getBotToken() {
		return this.botToken;
	}
	
	public String getIliasUsername() {
		return this.iliasUsername;
	}
	
	public String getIliasPasswort() {
		return this.iliasPasswort;
	}
	
	public Snowflake getGuildId() {
		return this.guildId;
	}
	
	public Snowflake getPlanChannelId() {
		return this.planChannelId;
	}
	
	public Snowflake getPlanTagId() {
		return this.planTagId;
	}
}