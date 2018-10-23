/*
 * This file is part of the logback-config project.
 * Copyright (c) 2017 Lucas Satabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnieh.logback.config;

import java.util.ArrayList;
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
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * A configurator using Typesafe's config library to lookup and load logger
 * configuration.
 *
 * @author Lucas Satabin
 *
 */
public class ConfigConfigurator extends ContextAwareBase implements Configurator {

	@Override
	public void configure(LoggerContext loggerContext) {

		this.setContext(loggerContext);

		BeanDescriptionCache beanCache = new BeanDescriptionCache(loggerContext);

		final Config config = ConfigFactory.load();
		// get the logback configuration root
		final String logbackConfigRoot = config.getString("logback-root");
		// load the configuration per config loading rules
		final Config logbackConfig = config.getConfig(logbackConfigRoot);

		final Config appenderConfigs = logbackConfig.getConfig("appenders");
		final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<>();
		for (Entry<String, ConfigValue> entry : appenderConfigs.root().entrySet()) {
			if (entry.getValue() instanceof ConfigObject) {
				try {
					appenders.put(entry.getKey(), configureAppender(loggerContext, entry.getKey(),
							appenderConfigs.getConfig("\"" + entry.getKey() + "\""), beanCache));
				} catch (Exception e) {
					addError(String.format("Unable to configure appender %s.", entry.getKey()), e);
				}
			} else {
				addWarn(String.format("Invalid appender configuration %s. Ignoring it.", entry.getKey()));
			}
		}

		if (logbackConfig.hasPath("root")) {
			if (logbackConfig.getValue("root") instanceof ConfigObject) {
				configureLogger(loggerContext, appenders, Logger.ROOT_LOGGER_NAME, logbackConfig.getConfig("root"), true);
			} else {
				addWarn("Invalid ROOT logger configuration. Ignoring it.");
			}
		}

		Config loggerConfigs = logbackConfig.getConfig("loggers");
		for (Entry<String, ConfigValue> entry : loggerConfigs.root().entrySet()) {
			if (entry.getValue() instanceof ConfigObject) {
				configureLogger(loggerContext, appenders, entry.getKey(),
						loggerConfigs.getConfig("\"" + entry.getKey() + "\""), false);
			} else {
				addWarn(String.format("Invalid logger configuration %s. Ignoring it.", entry.getKey()));
			}
		}
	}

	private Appender<ILoggingEvent> configureAppender(LoggerContext loggerContext, String name, Config config,
			BeanDescriptionCache beanCache) throws ReflectiveOperationException {
		List<Object> children = new ArrayList<>();

		@SuppressWarnings("unchecked")
		Class<Appender<ILoggingEvent>> clazz = (Class<Appender<ILoggingEvent>>) Class
				.forName(config.getString("class"));

		Appender<ILoggingEvent> appender = this.configureObject(loggerContext, clazz, config, children, beanCache);
		appender.setName(name);

		for (Object child : children) {
			if (child instanceof RollingPolicy) {
				((RollingPolicy) child).setParent((FileAppender<?>) appender);
			}
			if (child instanceof LifeCycle) {
				((LifeCycle) child).start();
			}
		}

		appender.start();
		return appender;

	}

	/**
	 * Configure an object of a given class.
	 *
	 * @param loggerContext
	 *            the context to assign to this object if it is
	 *            {@link ContextAwareBase}
	 * @param clazz
	 *            the class to instantiate
	 * @param config
	 *            a configuration containing the object's properties - each
	 *            top-level key except for "class" must have a corresponding setter
	 *            method, or an adder method in the case of lists
	 * @param children
	 *            a list which, if not null, will be filled with any child objects
	 *            assigned as properties
	 * @param beanCache
	 *            the bean cache instance
	 * @return the object instantiated with all properties assigned
	 * @throws ReflectiveOperationException
	 *             if any setter/adder method is missing or if the class cannot be
	 *             instantiated with a no-argument constructor
	 */
	private <T> T configureObject(LoggerContext loggerContext, Class<T> clazz, Config config, List<Object> children,
			BeanDescriptionCache beanCache) throws ReflectiveOperationException {
		T object = clazz.newInstance();

		if (object instanceof ContextAwareBase)
			((ContextAwareBase) object).setContext(loggerContext);

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, object);
		propertySetter.setContext(loggerContext);

		// file property (if any) must be set before any other property for appenders
		if (config.hasPath("file")) {
			propertySetter.setProperty("file", config);
		}

		for (Entry<String, ConfigValue> entry : config.withoutPath("class").withoutPath("file").root().entrySet()) {
			ConfigValue value = entry.getValue();
			switch (value.valueType()) {
			case OBJECT:
				Config subConfig = config.getConfig("\"" + entry.getKey() + "\"");
				if (subConfig.hasPath("class")) {
					Class<?> childClass = Class.forName(subConfig.getString("class"));
					Object child = this.configureObject(loggerContext, childClass, subConfig, null, beanCache);
					String propertyName = NameUtils.toLowerCamelCase(entry.getKey());
					propertySetter.setRawProperty(propertyName, child);
					if (children != null)
						children.add(child);
				} else {
					propertySetter.setProperty(entry.getKey(), config);
				}
				break;
			default:
				propertySetter.setProperty(entry.getKey(), config);
				break;
			}
		}

		return object;
	}

	private void configureLogger(LoggerContext loggerContext, Map<String, Appender<ILoggingEvent>> appenders,
			String name, Config config, boolean isRoot) {
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
