package org.gnieh.logback.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.PropertySetterException;

public class ConfigPropertySetter extends ContextAwareBase {

	private static final Class<?>[] STRING_CLASS_PARAMETER = new Class[] { String.class };

	private final Object obj;
	private final Class<?> objClass;
	private final BeanDescription beanDescription;

	public ConfigPropertySetter(BeanDescriptionCache beanDescriptionCache, Object obj) {
		this.obj = obj;
		this.objClass = obj.getClass();
		this.beanDescription = beanDescriptionCache.getBeanDescription(objClass);
	}

	public void setProperty(final String name, final Config config) {

		if (!config.hasPath(name)) {
			return;
		}

		String propertyName = NameUtils.toLowerCamelCase(name);

		switch (config.getValue(name).valueType()) {
		case LIST: {
			Method adder = findAdderMethod(propertyName);
			if (adder == null) {
				addWarn("No adder for property [" + name + "] in " + objClass.getName() + ".");
			} else {
				try {
					addProperty(adder, name, config);
				} catch (PropertySetterException ex) {
					addWarn("Failed to add property [" + name + "] to value \"" + config.getString(name) + "\". ", ex);
				}
			}
			break;
		}
		default: {
			Method setter = findSetterMethod(propertyName);
			if (setter == null) {
				addWarn("No setter for property [" + name + "] in " + objClass.getName() + ".");
			} else {
				try {
					setProperty(setter, name, config);
				} catch (PropertySetterException ex) {
					addWarn("Failed to set property [" + name + "] to value \"" + config.getString(name) + "\". ", ex);
				}
			}
			break;
		}
		}

	}

	public void setProperty(String name, Object complexProperty) {
		Method setter = findSetterMethod(name);

		if (setter == null) {
			addWarn("Not setter method for property [" + name + "] in " + obj.getClass().getName());

			return;
		}

		Class<?>[] paramTypes = setter.getParameterTypes();

		if (!isSanityCheckSuccessful(name, setter, paramTypes, complexProperty)) {
			return;
		}
		try {
			invokeMethodWithSingleParameterOnThisObject(setter, complexProperty);

		} catch (Exception e) {
			addError("Could not set component " + obj + " for parent component " + obj, e);
		}
	}

	private boolean isSanityCheckSuccessful(String name, Method method, Class<?>[] params, Object complexProperty) {
		Class<?> ccc = complexProperty.getClass();
		if (params.length != 1) {
			addError("Wrong number of parameters in setter method for property [" + name + "] in "
					+ obj.getClass().getName());

			return false;
		}

		if (!params[0].isAssignableFrom(complexProperty.getClass())) {
			addError("A \"" + ccc.getName() + "\" object is not assignable to a \"" + params[0].getName()
					+ "\" variable.");
			addError("The class \"" + params[0].getName() + "\" was loaded by ");
			addError("[" + params[0].getClassLoader() + "] whereas object of type ");
			addError("\"" + ccc.getName() + "\" was loaded by [" + ccc.getClassLoader() + "].");
			return false;
		}

		return true;
	}

	void invokeMethodWithSingleParameterOnThisObject(Method method, Object parameter) {
		Class<?> ccc = parameter.getClass();
		try {
			method.invoke(this.obj, parameter);
		} catch (Exception e) {
			addError("Could not invoke method " + method.getName() + " in class " + obj.getClass().getName()
					+ " with parameter of type " + ccc.getName(), e);
		}
	}

	private Method findSetterMethod(String propertyName) {
		return beanDescription.getSetter(propertyName);
	}

	private Method findAdderMethod(String propertyName) {
		return beanDescription.getAdder(propertyName);
	}

	@SuppressWarnings("unchecked")
	private void setProperty(Method setter, String name, Config config) throws PropertySetterException {
		Class<?>[] paramTypes = setter.getParameterTypes();

		final Object arg;

		try {

			Class<?> type = paramTypes[0];

			if (String.class.isAssignableFrom(type)) {
				arg = config.getString(name);
			} else if (Integer.TYPE.isAssignableFrom(type)) {
				arg = new Integer(config.getInt(name));
			} else if (Long.TYPE.isAssignableFrom(type)) {
				arg = new Long(config.getLong(name));
			} else if (Float.TYPE.isAssignableFrom(type)) {
				arg = new Float(config.getDouble(name));
			} else if (Double.TYPE.isAssignableFrom(type)) {
				arg = new Double(config.getDouble(name));
			} else if (Boolean.TYPE.isAssignableFrom(type)) {
				arg = new Boolean(config.getBoolean(name));
			} else if (Config.class.isAssignableFrom(type)) {
				arg = config.getConfig(name);
			} else if (Duration.class.isAssignableFrom(type)) {
				arg = config.getDuration(name);
			} else if (ConfigMemorySize.class.isAssignableFrom(type)) {
				arg = config.getMemorySize(name);
			} else if (type.isEnum()) {
				arg = convertToEnum(config.getString(name), (Class<? extends Enum<?>>) type);
			} else if (followsTheValueOfConvention(type)) {
				arg = convertByValueOfMethod(type, config.getString(name));
			} else if (isOfTypeCharset(type)) {
				arg = convertToCharset(config.getString(name));
			} else {
				arg = null;
			}

		} catch (Throwable t) {
			throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed. ", t);
		}

		if (arg == null) {
			throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed.");
		}
		try {
			setter.invoke(obj, arg);
		} catch (Exception ex) {
			throw new PropertySetterException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	private void addProperty(Method adder, String name, Config config) throws PropertySetterException {
		Class<?>[] paramTypes = adder.getParameterTypes();

		final List<? extends Object> arg;

		try {

			Class<?> type = paramTypes[0];

			if (String.class.isAssignableFrom(type)) {
				arg = config.getStringList(name);
			} else if (Integer.TYPE.isAssignableFrom(type)) {
				arg = config.getIntList(name);
			} else if (Long.TYPE.isAssignableFrom(type)) {
				arg = config.getLongList(name);
			} else if (Float.TYPE.isAssignableFrom(type)) {
				arg = config.getDoubleList(name);
			} else if (Double.TYPE.isAssignableFrom(type)) {
				arg = config.getDoubleList(name);
			} else if (Boolean.TYPE.isAssignableFrom(type)) {
				arg = config.getBooleanList(name);
			} else if (Config.class.isAssignableFrom(type)) {
				arg = config.getConfigList(name);
			} else if (Duration.class.isAssignableFrom(type)) {
				arg = config.getDurationList(name);
			} else if (ConfigMemorySize.class.isAssignableFrom(type)) {
				arg = config.getMemorySizeList(name);
			} else if (type.isEnum()) {
				arg = config.getStringList(name).stream().map(s -> convertToEnum(s, (Class<? extends Enum<?>>) type))
						.collect(Collectors.toList());
			} else if (followsTheValueOfConvention(type)) {
				arg = config.getStringList(name).stream().map(s -> convertByValueOfMethod(type, s))
						.collect(Collectors.toList());
			} else if (isOfTypeCharset(type)) {
				arg = config.getStringList(name).stream().map(s -> convertToCharset(s)).collect(Collectors.toList());
			} else {
				arg = Collections.emptyList();
			}
		} catch (Throwable t) {
			throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed. ", t);
		}

		if (arg == null) {
			throw new PropertySetterException("Conversion to type [" + paramTypes[0] + "] failed.");
		}
		try {
			for (Object o : arg) {
				adder.invoke(obj, o);
			}
		} catch (

		Exception ex) {
			throw new PropertySetterException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	private Object convertToEnum(String val, @SuppressWarnings("rawtypes") Class<? extends Enum> enumType) {
		return Enum.valueOf(enumType, val);
	}

	private boolean isOfTypeCharset(Class<?> type) {
		return Charset.class.isAssignableFrom(type);
	}

	private Charset convertToCharset(String val) {
		try {
			return Charset.forName(val);
		} catch (UnsupportedCharsetException e) {
			addError("Failed to get charset [" + val + "]", e);
			return null;
		}
	}

	private boolean followsTheValueOfConvention(Class<?> parameterClass) {
		try {
			Method valueOfMethod = parameterClass.getMethod(CoreConstants.VALUE_OF, STRING_CLASS_PARAMETER);
			int mod = valueOfMethod.getModifiers();
			if (Modifier.isStatic(mod)) {
				return true;
			}
		} catch (SecurityException e) {
			// nop
		} catch (NoSuchMethodException e) {
			// nop
		}
		return false;
	}

	private Object convertByValueOfMethod(Class<?> type, String val) {
		try {
			Method valueOfMethod = type.getMethod(CoreConstants.VALUE_OF, STRING_CLASS_PARAMETER);
			return valueOfMethod.invoke(null, val);
		} catch (Exception e) {
			addError("Failed to invoke " + CoreConstants.VALUE_OF + "{} method in class [" + type.getName()
					+ "] with value [" + val + "]");
			return null;
		}
	}

}
