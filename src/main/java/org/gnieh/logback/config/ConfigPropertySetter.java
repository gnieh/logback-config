package org.gnieh.logback.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.subst.NodeToStringTransformer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigMemorySize;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.util.beans.BeanDescription;
import ch.qos.logback.core.joran.util.beans.BeanDescriptionCache;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.PropertySetterException;

/**
 * General purpose Object property setter for setting properties via a
 * {@link Config}. Clients repeatedly invoke {@link #setProperty
 * setProperty(key,config)} in order to invoke setters on the Object specified
 * in the constructor.
 *
 * <p>
 * Usage:
 * <p>
 * Assume the configuration contains:
 *
 * <pre>
 * first-name = Joe
 * age = 32
 * male = true
 * </pre>
 *
 * The following code:
 *
 * <pre>
 * ConfigPropertySetter ps = new ConfigPropertySetter(beanCache, obj);
 * ps.setProperty(&quot;first-name&quot;, config);
 * ps.setProperty(&quot;age&quot;, config);
 * ps.setProperty(&quot;male&quot;, config);
 * </pre>
 *
 * will cause the invocations obj.setFirstName("Joe"), obj.setAge(32), and
 * obj.setMale(true) if methods exist with these signatures. Otherwise
 * appropriate warnings and/or errors are added to the associated
 * {@link Context}.
 *
 * <p>
 *
 * The client can also set a previously constructed object directly via
 * {@link #setRawProperty(String, Object)}.
 */
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

	/**
	 * Set the property corresponding to a given key in the provided Config.
	 * Configuration keys are mangled to a property name with following rule:
	 * any dash (-) followed by a letter is mangled into the uppercased letter.
	 * For example, configuration key my-property-name is mangled as
	 * myPropertyName.
	 */
	public void setProperty(final String key, final Config config, final Context context, final ConfigAppendersCache appendersCache) {

		if (!config.hasPath(key)) {
			return;
		}

		String propertyName = NameUtils.toLowerCamelCase(key);

		switch (config.getValue(key).valueType()) {
		case LIST: {
			Method adder = findAdderMethod(singularize(propertyName));
			if (adder == null) {
				addWarn("No adder for property [" + key + "] in " + objClass.getName() + ".");
			} else {
				try {
					addProperty(adder, key, config, appendersCache);
				} catch (PropertySetterException ex) {
					addWarn("Failed to add property [" + key + "] to value \"" + config.getValue(key) + "\". ", ex);
				}
			}
			break;
		}
		default: {
			Method setter = findSetterMethod(propertyName);
			if (setter == null) {
				addWarn("No setter for property [" + key + "] in " + objClass.getName() + ".");
			} else {
				try {
					setProperty(setter, key, config, context);
				} catch (PropertySetterException ex) {
					addWarn("Failed to set property [" + key + "] to value \"" + config.getValue(key) + "\". ", ex);
				}
			}
			break;
		}
		}

	}

	/**
	 * Set a property directly using the property name and a previously
	 * constructed object.
	 */
	public void setRawProperty(String propertyName, Object complexProperty) {
		Method setter = findSetterMethod(propertyName);

		if (setter == null) {
			addWarn("Not setter method for property [" + propertyName + "] in " + obj.getClass().getName());

			return;
		}

		Class<?>[] paramTypes = setter.getParameterTypes();

		if (!isSanityCheckSuccessful(propertyName, setter, paramTypes, complexProperty)) {
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
	private void setProperty(Method setter, String name, Config config, Context context) throws PropertySetterException {
		Class<?>[] paramTypes = setter.getParameterTypes();

		final Object arg;

		try {

			Class<?> type = paramTypes[0];

			if (String.class.isAssignableFrom(type)) {
				arg = NodeToStringTransformer.substituteVariable(config.getString(name), context, null);
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
				final String subst = NodeToStringTransformer.substituteVariable(config.getString(name), context, null);
				arg = convertToEnum(subst, (Class<? extends Enum<?>>) type);
			} else if (followsTheValueOfConvention(type)) {
				final String subst = NodeToStringTransformer.substituteVariable(config.getString(name), context, null);
				arg = convertByValueOfMethod(type, subst);
			} else if (isOfTypeCharset(type)) {
				final String subst = NodeToStringTransformer.substituteVariable(config.getString(name), context, null);
				arg = convertToCharset(subst);
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
	private void addProperty(Method adder, String name, Config config, ConfigAppendersCache appendersCache) throws PropertySetterException {
		Class<?>[] paramTypes = adder.getParameterTypes();

		final List<? extends Object> arg;

		try {

			Class<?> type = paramTypes[0];

			if (String.class.isAssignableFrom(type)) {
				final List<String> strings = config.getStringList(name);
				final List<String> result = new ArrayList<>(strings.size());
				for(String s : strings) {
					result.add(NodeToStringTransformer.substituteVariable(s, context, null));
				}
				arg = result;
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
				final List<String> strings = config.getStringList(name);
				final List<Object> result = new ArrayList<>(strings.size());
				for(String s : strings) {
					final String subst = NodeToStringTransformer.substituteVariable(s, context, null);
					result.add(convertToEnum(subst, (Class<? extends Enum<?>>) type));
				}
				arg = result;
			} else if (followsTheValueOfConvention(type)) {
				final List<String> strings = config.getStringList(name);
				final List<Object> result = new ArrayList<>(strings.size());
				for(String s : strings) {
					final String subst = NodeToStringTransformer.substituteVariable(s, context, null);
					result.add(convertByValueOfMethod(type, subst));
				}
				arg = result;
			} else if (isOfTypeCharset(type)) {
				final List<String> strings = config.getStringList(name);
				final List<Object> result = new ArrayList<>(strings.size());
				for(String s : strings) {
					final String subst = NodeToStringTransformer.substituteVariable(s, context, null);
					result.add(convertToCharset(subst));
				}
				arg = result;
			} else if (Appender.class.isAssignableFrom(type)) {
				final List<String> appenderNames = config.getStringList(name);
				final List<Appender<ILoggingEvent>> referencedAppenders = new ArrayList<>(appenderNames.size());
				for (String appenderName : appenderNames) {
					referencedAppenders.add(appendersCache.getAppender(appenderName));
				}
				arg = referencedAppenders;
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

	private String singularize(String s) {
		if (s.charAt(s.length() - 1) == 's') {
			return s.substring(0, s.length() - 1);
		}
		addWarn("Failed to singularize property name " + s);
		return s;
	}

}
