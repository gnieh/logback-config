package org.gnieh.logback.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;

public class TestBean {

	private int intProperty;

	private long longProperty;

	private float floatProperty;

	private double doubleProperty;

	private String stringProperty;

	private Duration duration;

	private ConfigMemorySize size;

	private Config subConfig;

	private TestEnum enumProperty;

	private List<Integer> ints = new ArrayList<>();

	private List<Appender<ILoggingEvent>> appenders = new ArrayList<>();

	public int getIntProperty() {
		return intProperty;
	}

	public void setIntProperty(int intProperty) {
		this.intProperty = intProperty;
	}

	public double getDoubleProperty() {
		return doubleProperty;
	}

	public void setDoubleProperty(double doubleProperty) {
		this.doubleProperty = doubleProperty;
	}

	public long getLongProperty() {
		return longProperty;
	}

	public void setLongProperty(long longProperty) {
		this.longProperty = longProperty;
	}

	public float getFloatProperty() {
		return floatProperty;
	}

	public void setFloatProperty(float floatProperty) {
		this.floatProperty = floatProperty;
	}

	public String getStringProperty() {
		return stringProperty;
	}

	public void setStringProperty(String stringProperty) {
		this.stringProperty = stringProperty;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public ConfigMemorySize getSize() {
		return size;
	}

	public void setSize(ConfigMemorySize size) {
		this.size = size;
	}

	public TestEnum getEnumProperty() {
		return enumProperty;
	}

	public void setEnumProperty(TestEnum enumProperty) {
		this.enumProperty = enumProperty;
	}

	public Config getSubConfig() {
		return subConfig;
	}

	public void setSubConfig(Config subConfig) {
		this.subConfig = subConfig;
	}

	public void addInt(int i) {
		ints.add(i);
	}

	public List<Integer> getInts() {
		return ints;
	}

	public void addAppender(Appender<ILoggingEvent> appender) {
		appenders.add(appender);
	}

	public List<Appender<ILoggingEvent>> getAppenders() {
		return appenders;
	}

}
