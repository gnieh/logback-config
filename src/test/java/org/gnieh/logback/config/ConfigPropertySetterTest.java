package org.gnieh.logback.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigMemorySize;

import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;

public class ConfigPropertySetterTest {

	private BeanDescriptionCache beanCache = new BeanDescriptionCache();

	@Test
	public void testIntProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0, bean.getIntProperty());

		propertySetter.setProperty("int-property", config);

		Assert.assertEquals(12, bean.getIntProperty());

	}

	@Test
	public void testLongProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0l, bean.getLongProperty());

		propertySetter.setProperty("long-property", config);

		Assert.assertEquals(120000000000000000l, bean.getLongProperty());

	}

	@Test
	public void testFloatProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0.0f, bean.getFloatProperty(), 0.0f);

		propertySetter.setProperty("float-property", config);

		Assert.assertEquals(1.2f, bean.getFloatProperty(), 0.0f);

	}

	@Test
	public void testDoubleProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(0.0d, bean.getDoubleProperty(), 0.0d);

		propertySetter.setProperty("double-property", config);

		Assert.assertEquals(1.1d, bean.getDoubleProperty(), 0.0d);

	}

	@Test
	public void testStringProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getStringProperty());

		propertySetter.setProperty("stringProperty", config);

		Assert.assertEquals("This is a test", bean.getStringProperty());

	}

	@Test
	public void testDurationProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getDuration());

		propertySetter.setProperty("duration", config);

		Assert.assertEquals(Duration.ofDays(12), bean.getDuration());

	}

	@Test
	public void testSizeProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getSize());

		propertySetter.setProperty("size", config);

		Assert.assertEquals(ConfigMemorySize.ofBytes(2048l * 1024l * 1024l), bean.getSize());

	}

	@Test
	public void testSubConfigProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getSubConfig());

		propertySetter.setProperty("sub-config", config);

		Assert.assertEquals(config.getConfig("sub-config"), bean.getSubConfig());

	}

	@Test
	public void testEnumProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(null, bean.getEnumProperty());

		propertySetter.setProperty("enum-property", config);

		Assert.assertEquals(TestEnum.VALUE1, bean.getEnumProperty());

	}

	@Test
	public void testIntListProperty() {

		TestBean bean = new TestBean();

		ConfigPropertySetter propertySetter = new ConfigPropertySetter(beanCache, bean);

		Config config = ConfigFactory.load("bean");

		Assert.assertEquals(Collections.emptyList(), bean.getInts());

		propertySetter.setProperty("ints", config);

		List<Integer> l = new ArrayList<>();
		l.add(1);
		l.add(2);
		l.add(3);
		l.add(4);
		l.add(5);
		Assert.assertEquals(l, bean.getInts());

	}

}
