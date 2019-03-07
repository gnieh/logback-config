package org.gnieh.logback.config;

import java.time.Duration;
import java.util.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import org.junit.Assert;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigMemorySize;

import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;

public class ConfigPropertySetterTest {

	private BeanDescriptionCache beanCache = new BeanDescriptionCache(null);

	@Test
	public void testIntProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0, bean.getIntProperty());

		propertySetter.setProperty("int-property", config, null, null);

		Assert.assertEquals(12, bean.getIntProperty());

	}

	@Test
	public void testLongProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0l, bean.getLongProperty());

		propertySetter.setProperty("long-property", config, null, null);

		Assert.assertEquals(120000000000000000l, bean.getLongProperty());

	}

	@Test
	public void testFloatProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0.0f, bean.getFloatProperty(), 0.0f);

		propertySetter.setProperty("float-property", config, null, null);

		Assert.assertEquals(1.2f, bean.getFloatProperty(), 0.0f);

	}

	@Test
	public void testDoubleProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0.0d, bean.getDoubleProperty(), 0.0d);

		propertySetter.setProperty("double-property", config, null, null);

		Assert.assertEquals(1.1d, bean.getDoubleProperty(), 0.0d);

	}

	@Test
	public void testStringProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getStringProperty());

		propertySetter.setProperty("stringProperty", config, null, null);

		Assert.assertEquals("This is a test", bean.getStringProperty());

	}

	@Test
	public void testDurationProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getDuration());

		propertySetter.setProperty("duration", config, null, null);

		Assert.assertEquals(Duration.ofDays(12), bean.getDuration());

	}

	@Test
	public void testSizeProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getSize());

		propertySetter.setProperty("size", config, null, null);

		Assert.assertEquals(ConfigMemorySize.ofBytes(2048l * 1024l * 1024l), bean.getSize());

	}

	@Test
	public void testSubConfigProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getSubConfig());

		propertySetter.setProperty("sub-config", config, null, null);

		Assert.assertEquals(config.getConfig("sub-config"), bean.getSubConfig());

	}

	@Test
	public void testEnumProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getEnumProperty());

		propertySetter.setProperty("enum-property", config, null, null);

		Assert.assertEquals(TestEnum.VALUE1, bean.getEnumProperty());

	}

	@Test
	public void testIntListProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(Collections.emptyList(), bean.getInts());

		propertySetter.setProperty("ints", config, null, null);

		List<Integer> l = new ArrayList<>();
		l.add(1);
		l.add(2);
		l.add(3);
		l.add(4);
		l.add(5);
		Assert.assertEquals(l, bean.getInts());

	}

	@Test
	public void testAppenderListProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(Collections.emptyList(), bean.getAppenders());

		Map<String, Appender<ILoggingEvent>> otherAppenders = new HashMap<>();
		Appender<ILoggingEvent> testAppender1 = new FileAppender<>();
		Appender<ILoggingEvent> testAppender2 = new ConsoleAppender<>();
		otherAppenders.put("test-appender-1", testAppender1);
		otherAppenders.put("test-appender-2", testAppender2);

		ConfigAppendersCache appendersCache = new ConfigAppendersCache();
		appendersCache.setLoader(otherAppenders::get);

		propertySetter.setProperty("appenders", config, null, appendersCache);

		List<Appender<ILoggingEvent>> l = new ArrayList<>();
		l.add(testAppender1);
		l.add(testAppender2);
		Assert.assertEquals(l, bean.getAppenders());

	}

}
