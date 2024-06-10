package de.ngloader.referee;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class RefereeLogger {

	public static Logger LOGGER = Logger.getLogger("Referee");

	public static void debug(String message) {
		RefereeLogger.LOGGER.log(Level.INFO, "[Debug] " + message);
	}

	public static void info(String message) {
		log(Level.INFO, message);
	}

	public static void warn(String message) {
		log(Level.WARNING, message);
	}

	public static void error(Throwable e) {
		log(Level.SEVERE, e.getMessage(), e);
	}

	public static void error(String message, Throwable e) {
		log(Level.SEVERE, message, e);
	}

	public static void log(Level level, String message) {
		RefereeLogger.LOGGER.log(level, message);
	}

	public static void log(Level level, String message, Throwable throwable) {
		RefereeLogger.LOGGER.log(level, message, throwable);
	}
	
	private RefereeLogger() { }
}