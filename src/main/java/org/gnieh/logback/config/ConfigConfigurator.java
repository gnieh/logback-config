/*
 * This file is part of the logback-config project.
 * Copyright (c) 2018 Lucas Satabin
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jmx.JMXConfigurator;
import ch.qos.logback.classic.jmx.MBeanUtil;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A configurator using Typesafe's config library to lookup and load logger
 * configuration.
 *
 * @author Lucas Satabin
 */
public class ConfigConfigurator extends ContextAwareBase implements Configurator {

    @Override
    public void configure(LoggerContext loggerContext) {

        this.setContext(loggerContext);

        BeanDescriptionCache beanCache = new BeanDescriptionCache(loggerContext);


        ConfigLoader loader = getLoader();
        Config config;
        try {
            config = loader.load();
        } catch (Throwable t) {
            addError("Unable to load Typesafe config", t);
            return;
        }

        // get the logback configuration root
        final String logbackConfigRoot = config.getString("logback-root");
        // load the configuration per config loading rules
        final Config logbackConfig = config.getConfig(logbackConfigRoot);

        final Config appenderConfigs = logbackConfig.getConfig("appenders");
        final ConfigAppendersCache appendersCache = new ConfigAppendersCache();
        appendersCache.setLoader(name -> configureAppender(loggerContext, name, appenderConfigs.getConfig("\"" + name + "\""), beanCache, appendersCache));
        final Map<String, Appender<ILoggingEvent>> appenders = new HashMap<>();
        for (Entry<String, ConfigValue> entry : appenderConfigs.root().entrySet()) {
            if (entry.getValue() instanceof ConfigObject) {
                try {
                    appenders.put(entry.getKey(), appendersCache.getAppender(entry.getKey()));
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

        if (logbackConfig.hasPath("jmx-configurator")) {
            final Config jmxConfig = logbackConfig.getConfig("jmx-configurator");
            final String contextName;
            if (jmxConfig.hasPath("context-name")) {
                contextName = jmxConfig.getString("context-name");
            } else {
                contextName = loggerContext.getName();
            }

            final String objectNameAsStr;
            if (jmxConfig.hasPath("object-name")) {
                objectNameAsStr = jmxConfig.getString("object-name");
            } else {
                objectNameAsStr = MBeanUtil.getObjectNameFor(contextName, JMXConfigurator.class);
            }

            ObjectName objectName = MBeanUtil.string2ObjectName(loggerContext, this, objectNameAsStr);
            if (objectName == null) {
                addError("Failed construct ObjectName for [" + objectNameAsStr + "]");
                return;
            }

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            if (!MBeanUtil.isRegistered(mbs, objectName)) {
                // register only of the named JMXConfigurator has not been previously
                // registered. Unregistering an MBean within invocation of itself
                // caused jconsole to throw an NPE. (This occurs when the reload* method
                // unregisters the
                JMXConfigurator jmxConfigurator = new JMXConfigurator(loggerContext, mbs, objectName);
                try {
                    mbs.registerMBean(jmxConfigurator, objectName);
                } catch (Exception e) {
                    addError("Failed to create mbean", e);
                }
            }
        }

        // Use a LinkedHashSet so order is preserved. We use the first one in as the 'main' URL, under the assumption
        // that maybe that matters somehow to logback. The way we traverse the config tree means that the first one we
        // add will be one that is closer to the root of the tree.
        if (registerFileWatchers(loggerContext, getSourceFiles(config.root(), new LinkedHashSet<>()))) {
            createChangeTask(loggerContext, logbackConfig);
        }
    }

    /**
     * Get the correct config factory object.
     *
     * @return the config factory
     */
    private ConfigLoader getLoader() {
        Iterator<ConfigLoader> loaders = ServiceLoader.load(ConfigLoader.class).iterator();
        if (loaders.hasNext()) {
            return loaders.next();
        }
        return this::defaultLoader;
    }

    /**
     * Default config loading method.
     *
     * @return the basic TS-config loading
     */
    private Config defaultLoader() {
        return ConfigFactory.load();
    }

    private Appender<ILoggingEvent> configureAppender(LoggerContext loggerContext, String name, Config config,
                                                      BeanDescriptionCache beanCache, ConfigAppendersCache appendersCache) throws ReflectiveOperationException {
        List<Object> children = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Class<Appender<ILoggingEvent>> clazz = (Class<Appender<ILoggingEvent>>) Class
                .forName(config.getString("class"));

        Appender<ILoggingEvent> appender = this.configureObject(loggerContext, clazz, config, children, beanCache, appendersCache);
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
     * @param loggerContext  the context to assign to this object if it is
     *                       {@link ContextAwareBase}
     * @param clazz          the class to instantiate
     * @param config         a configuration containing the object's properties - each
     *                       top-level key except for "class" must have a corresponding setter
     *                       method, or an adder method in the case of lists
     * @param children       a list which, if not null, will be filled with any child objects
     *                       assigned as properties
     * @param beanCache      the bean cache instance
     * @param appendersCache the cache of references to other appenders
     * @return the object instantiated with all properties assigned
     * @throws ReflectiveOperationException if any setter/adder method is missing or if the class cannot be
     *                                      instantiated with a no-argument constructor
     */
    private <T> T configureObject(LoggerContext loggerContext, Class<T> clazz, Config config, List<Object> children,
                                  BeanDescriptionCache beanCache, ConfigAppendersCache appendersCache) throws ReflectiveOperationException {
        T object = clazz.newInstance();

        if (object instanceof ContextAwareBase)
            ((ContextAwareBase) object).setContext(loggerContext);

        ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, object);
        propertySetter.setContext(loggerContext);

        // file property (if any) must be set before any other property for appenders
        if (config.hasPath("file")) {
            propertySetter.setProperty("file", config, loggerContext, appendersCache);
        }

        for (Entry<String, ConfigValue> entry : config.withoutPath("class").withoutPath("file").root().entrySet()) {
            ConfigValue value = entry.getValue();
            switch (value.valueType()) {
                case OBJECT:
                    Config subConfig = config.getConfig("\"" + entry.getKey() + "\"");
                    if (subConfig.hasPath("class")) {
                        Class<?> childClass = Class.forName(subConfig.getString("class"));
                        Object child = this.configureObject(loggerContext, childClass, subConfig, null, beanCache, appendersCache);
                        String propertyName = NameUtils.toLowerCamelCase(entry.getKey());
                        propertySetter.setRawProperty(propertyName, child);
                        if (children != null)
                            children.add(child);
                    } else {
                        propertySetter.setProperty(entry.getKey(), config, loggerContext, appendersCache);
                    }
                    break;
                default:
                    propertySetter.setProperty(entry.getKey(), config, loggerContext, appendersCache);
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

    /**
     * Find all real source files in the config. This does not include those encapsulated in jars, etc. Only those that
     * are directly in the file system.
     *
     * @param config the TS-config
     * @param files  the set to add newly found files to
     *
     * @return the set of files found, as URLs
     */
    private Set<URL> getSourceFiles(ConfigValue config, Set<URL> files) {
        if (config.origin().filename() != null) {
            // Check 'contains' first to avoid re-adding, and messing with the order
            if (!files.contains(config.origin().url())) {
                files.add(config.origin().url());
            }
        }
        if (config.valueType() == ConfigValueType.OBJECT) {
            for (ConfigValue value : ((ConfigObject)config).values()) {
                getSourceFiles(value, files);
            }
        }
        return files;
    }

    /**
     * Register all real config files to be watched by logback.
     *
     * @param loggerContext the logger context
     * @param sourceFiles   the source files to watch
     *
     * @return true if there were any files to watch
     */
    private boolean registerFileWatchers(LoggerContext loggerContext, Set<URL> sourceFiles) {
        Iterator<URL> iterator = sourceFiles.iterator();
        if (iterator.hasNext()) {
            ConfigurationWatchListUtil.setMainWatchURL(loggerContext, iterator.next());
            iterator.forEachRemaining(u -> {
                if (u != null) {
                    ConfigurationWatchListUtil.addToWatchList(loggerContext, u);
                }
            });
            return true;
        }
        addInfo("No configuration files to watch, so no file scanning is possible");
        return false;
    }

    /**
     * Create and schedule the task to check for changes to the watch list.
     *
     * @param loggerContext the logger context
     * @param config        the logback TS-config
     */
    private void createChangeTask(LoggerContext loggerContext, Config config) {
        if (config.hasPath("scan-period") && !config.getIsNull("scan-period")) {
            long delay = config.getDuration("scan-period", TimeUnit.MILLISECONDS);
            if (delay > 0) {
                Runnable rocTask = () -> {
                    ConfigurationWatchList configurationWatchList =
                            ConfigurationWatchListUtil.getConfigurationWatchList(loggerContext);
                    if (configurationWatchList == null) {
                        addWarn("Null ConfigurationWatchList in context");
                        return;
                    }
                    List<File> filesToWatch = configurationWatchList.getCopyOfFileWatchList();
                    if (filesToWatch == null || filesToWatch.isEmpty()) {
                        addInfo("Empty watch file list. Disabling ");
                        return;
                    }
                    if (!configurationWatchList.changeDetected()) {
                        return;
                    }
                    loggerContext.reset();
                    configure(loggerContext);
                };

                loggerContext.putObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK, rocTask);
                loggerContext.addScheduledFuture(loggerContext.getScheduledExecutorService().
                        scheduleAtFixedRate(rocTask, delay, delay, TimeUnit.MILLISECONDS));
            }
        }
    }

}
