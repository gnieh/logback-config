package org.gnieh.logback.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.util.PropertySetter;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ContextAwareBase;

// TODO use our own PropertySetter based on the powerful conversions performed by config

/**
 * A configurator using Typesafe's config library to lookup and load logger
 * configruation.
 * 
 * @author Lucas Satabin
 *
 */
public class ConfigConfigurator extends ContextAwareBase implements Configurator {

	private BeanDescriptionCache beanCache = new BeanDescriptionCache();

	@Override
	public void configure(LoggerContext loggerContext) {

		// load the configuration per config loading rules
		final Config config = ConfigFactory.load();

		final Config appenderConfigs = config.getConfig("logging.appenders");
		final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<>();
		for (Entry<String, ConfigValue> entry : appenderConfigs.entrySet()) {
			if (entry.getValue() instanceof ConfigObject) {
				try {
					appenders.put(entry.getKey(), configureAppender(loggerContext, entry.getKey(), appenderConfigs.getConfig(entry.getKey())));
				} catch (Exception e) {
					addError(String.format("Unable to configure appender %s.", entry.getKey()), e);
				}
			} else {
				addWarn(String.format("Invalid appender configuration %s. Ignoring it.", entry.getKey()));
			}
		}

		if (config.hasPath("logging.root")) {
			if (config.getValue("logging.root") instanceof ConfigObject) {
				configureLogger(loggerContext, appenders, Logger.ROOT_LOGGER_NAME, config.getConfig("logging.root"), true);
			} else {
				addWarn("Invalid ROOT logger configuration. Ignoring it.");
			}

		}

		Config loggerConfigs = config.getConfig("logging.loggers");
		for (Entry<String, ConfigValue> entry : loggerConfigs.entrySet()) {
			if (entry.getValue() instanceof ConfigObject) {
				configureLogger(loggerContext, appenders, entry.getKey(), loggerConfigs.getConfig(entry.getKey()), false);
			} else {
				addWarn(String.format("Invalid logger configuration %s. Ignoring it.", entry.getKey()));
			}
		}

	}

	private Appender<ILoggingEvent> configureAppender(LoggerContext loggerContext, String name, Config config)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		Class<?> clazz = Class.forName(config.getString("class"));
		@SuppressWarnings("unchecked")
		Appender<ILoggingEvent> appender = (Appender<ILoggingEvent>) clazz.newInstance();

		appender.setName(name);

		// for each key appearing in the appender configuration, check whether
		// it is a bean property and call the correct method on the appender
		PropertySetter propertySetter = new PropertySetter(beanCache, appender);
		for (Entry<String, ConfigValue> entry : config.withoutPath("class").entrySet()) {
			String propertyName = NameUtils.toLowerCamelCase(entry.getKey());
			if ("encoder".equals(propertyName)) {
				Encoder<?> encoder = configureEncoder(loggerContext, config.getConfig("encoder"));
				propertySetter.setComplexProperty(propertyName, encoder);
			} else if ("layout".equals(propertyName)) {
				Layout<?> layout = configureLayout(loggerContext, config.getConfig("layout"));
				propertySetter.setComplexProperty(propertyName, layout);
			} else if ("filter".equals(propertyName)) {
				Filter<?> filter = configureFilter(loggerContext, config.getConfig("filter"));
				propertySetter.setComplexProperty(propertyName, filter);
			} else if ("filters".equals(propertyName)) {
				for (Config c : config.getConfigList("filters")) {
					Filter<?> filter = configureFilter(loggerContext, c);
					propertySetter.setComplexProperty(propertyName, filter);
				}
			} else {
				Object value = entry.getValue().unwrapped();
				if (value instanceof String) {
					propertySetter.setProperty(propertyName, (String) value);
				} else {
					propertySetter.setComplexProperty(propertyName, value);
				}
			}
		}

		appender.setContext(loggerContext);
		appender.start();
		return appender;

	}

	private Layout<?> configureLayout(LoggerContext loggerContext, Config config)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class<?> clazz = Class.forName(config.getString("class"));
		Layout<?> layout = (Layout<?>) clazz.newInstance();

		// for each key appearing in the layout configuration, check whether
		// it is a bean property and call the correct method on the layout
		PropertySetter propertySetter = new PropertySetter(beanCache, layout);
		for (Entry<String, ConfigValue> entry : config.withoutPath("class").entrySet()) {
			String propertyName = NameUtils.toLowerCamelCase(entry.getKey());

			Object value = entry.getValue().unwrapped();
			if (value instanceof String) {
				propertySetter.setProperty(propertyName, (String) value);
			} else {
				propertySetter.setComplexProperty(propertyName, value);
			}
		}

		layout.setContext(loggerContext);
		layout.start();

		return layout;
	}

	private Filter<?> configureFilter(LoggerContext loggerContext, Config config)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class<?> clazz = Class.forName(config.getString("class"));
		Filter<?> filter = (Filter<?>) clazz.newInstance();

		// for each key appearing in the filter configuration, check whether
		// it is a bean property and call the correct method on the filter
		PropertySetter propertySetter = new PropertySetter(beanCache, filter);
		for (Entry<String, ConfigValue> entry : config.withoutPath("class").entrySet()) {
			String propertyName = NameUtils.toLowerCamelCase(entry.getKey());

			Object value = entry.getValue().unwrapped();
			if (value instanceof String) {
				propertySetter.setProperty(propertyName, (String) value);
			} else {
				propertySetter.setComplexProperty(propertyName, value);
			}
		}

		filter.setContext(loggerContext);
		filter.start();

		return filter;
	}

	private Encoder<?> configureEncoder(LoggerContext loggerContext, Config config)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(config.getString("class"));
		Encoder<?> encoder = (Encoder<?>) clazz.newInstance();

		// for each key appearing in the encoder configuration, check whether
		// it is a bean property and call the correct method on the encoder
		PropertySetter propertySetter = new PropertySetter(beanCache, encoder);
		for (Entry<String, ConfigValue> entry : config.withoutPath("class").entrySet()) {
			String propertyName = NameUtils.toLowerCamelCase(entry.getKey());
			if ("layout".equals(propertyName)) {
				Layout<?> layout = configureLayout(loggerContext, config.getConfig("layout"));
				propertySetter.setComplexProperty(propertyName, layout);
			} else {
				Object value = entry.getValue().unwrapped();
				if (value instanceof String) {
					propertySetter.setProperty(propertyName, (String) value);
				} else {
					propertySetter.setComplexProperty(propertyName, value);
				}
			}
		}

		encoder.setContext(loggerContext);
		encoder.start();

		return encoder;
	}

	private void configureLogger(LoggerContext loggerContext, Map<String, Appender<ILoggingEvent>> appenders, String name, Config config, boolean isRoot) {
		final Logger logger = loggerContext.getLogger(name);

		if (config.hasPathOrNull("level")) {
			if (config.getIsNull("level") && isRoot) {
				addWarn("Log level NULL is not authorized for ROOT logger");
			} else if (!config.getIsNull("level")) {
				String levelName = config.getString("level");
				if (isRoot && (levelName.equalsIgnoreCase("NULL") || levelName.equalsIgnoreCase("INHERITED"))) {
					addWarn(String.format("Log level %s is not authorized for ROOT logger.", levelName.toUpperCase()));
				} else if (!levelName.equalsIgnoreCase("NULL") && !levelName.equalsIgnoreCase("INHERITED")) {
					logger.setLevel(Level.toLevel(config.getString("level")));
				}
			}
		}

		if (config.hasPath("additivity")) {
			logger.setAdditive(config.getBoolean("additivity"));
		}

		if (config.hasPath("appenders")) {
			List<String> appenderRefs = config.getStringList("appenders");
			for (String appenderRef : appenderRefs) {
				if (appenders.containsKey(appenderRef)) {
					logger.addAppender(appenders.get(appenderRef));
				} else {
					addWarn(String.format("Unknown appender %s. Ignoring it.", appenderRef));
				}
			}
		}

	}

}
