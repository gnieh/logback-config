package org.gnieh.logback.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameUtils {

	private NameUtils() {
	}

	public static String toLowerCamelCase(String string) {
		if (string == null || string.isEmpty()) {
			return string;
		}

		// convert all '-' followed by a letter to this letter in upper case
		Matcher m = Pattern.compile("-(\\w?)").matcher(string);
		StringBuilder sb = new StringBuilder();
		int last = 0;
		while (m.find()) {
			sb.append(string.substring(last, m.start()));
			sb.append(m.group(1).toUpperCase());
			last = m.end();
		}
		sb.append(string.substring(last));

		String string2 = sb.toString();

		if (string2.length() > 1 && Character.isUpperCase(string2.charAt(1)) && Character.isUpperCase(string2.charAt(0))) {
			return string2;
		}
		char chars[] = string2.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);

		return new String(chars);

	}

}
