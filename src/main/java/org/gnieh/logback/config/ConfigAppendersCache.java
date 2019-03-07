package org.gnieh.logback.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import java.util.HashMap;
import java.util.Map;

/**
 * A lazy cache that supports appender references.
 * If appender X references another appender Y - then the cache will load Y and set it as one of the properties of X.
 *
 * @author NiceBKB
 */
public class ConfigAppendersCache {
	/**
	 * The map of appender names to loaded appender objects
	 */
	private final Map<String, Appender<ILoggingEvent>> cache = new HashMap<>();
	/**
	 * Function that loads the appender from the appender name
	 */
	private AppenderLoader loader;

	/**
	 * Assigns the loading function.
	 *
	 * @param loader the function to be used to load the appender by name
	 */
	public void setLoader(AppenderLoader loader) {
		this.loader = loader;
	}

	/**
	 * Provides the existing appender or loads the new one by appender name.
	 *
	 * @param name the name of the appender to load
	 * @return the loaded appender
	 */
	public Appender<ILoggingEvent> getAppender(String name) throws ReflectiveOperationException {
		if (cache.containsKey(name)) {
			return cache.get(name);
		} else {
			Appender<ILoggingEvent> appender = loader.load(name);
			cache.put(name, appender);
			return appender;
		}
	}

	/**
	 * Wraps the function for loading appenders by name.
	 */
	interface AppenderLoader {
		/**
		 * Loads the appender by name.
		 *
		 * @param name the name of the appender to load
		 * @return the loaded appender
		 * @throws ReflectiveOperationException when configuring appender fails
		 */
		Appender<ILoggingEvent> load(String name) throws ReflectiveOperationException;
	}
}