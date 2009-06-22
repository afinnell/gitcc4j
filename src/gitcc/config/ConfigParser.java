package gitcc.config;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ConfigParser {

	private static final String SEP = "\\|";

	public void parseConfig(Config config, File root) throws Exception {
		File file = new File(root, ".git/gitcc");
		parseConfig(config, new FileReader(file));
	}

	protected void parseConfig(Config config, Reader reader) throws Exception {
		ConfigSetter setter = new ConfigSetter(config);
		String mode = "core";
		BufferedReader in = new BufferedReader(reader);
		for (String line; (line = in.readLine()) != null;) {
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#"))
				continue;
			if (line.startsWith("[")) {
				mode = line.substring(1, line.length() - 1);
			} else {
				String[] values = line.split("=", 2);
				if ("core".equals(mode) || mode.equals(config._getBranch())) {
					setter.set(values[0].trim(), values[1].trim());
				}
			}
		}
	}

	private static class ConfigSetter {
		private Map<String, Method> methods = new HashMap<String, Method>();
		private Config config;

		public ConfigSetter(Config config) {
			this.config = config;
			init();
		}

		private void init() {
			BeanInfo beanInfo;
			try {
				beanInfo = Introspector.getBeanInfo(config.getClass());
			} catch (IntrospectionException e) {
				throw new RuntimeException(e);
			}
			for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
				methods.put(processName(pd.getName()), pd.getWriteMethod());
			}
		}

		public void set(String name, String svalue) {
			Method m = methods.get(name);
			if (m == null)
				return;
			Object value = svalue;
			Class<?> type = m.getParameterTypes()[0];
			if (String[].class.isAssignableFrom(type))
				value = svalue.split(SEP);
			try {
				m.invoke(config, value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

	protected static String processName(String name) {
		String[] split = name.split("\\.");
		StringBuilder b = new StringBuilder();
		b.append(split[0]);
		for (int i = 1; i < split.length; i++) {
			b.append(Character.toUpperCase(split[i].charAt(0)));
			b.append(split[i].substring(1));
		}
		return b.toString();
	}
}