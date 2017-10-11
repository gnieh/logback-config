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
