/**
 *
 */
package org.tfelab.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Dectinc
 * @version Aug 29, 2014 8:54:25 AM
 */
public class PropertiesHolder {

	private static final Logger logger = LogManager.getLogger(PropertiesHolder.class
			.getName());
	private Properties p = null;

	/**
	 * @param file
	 */
	public PropertiesHolder(String file) {
		try {

			String configPath = System.getProperty("user.dir") + File.separator
					+ file;
			logger.info("Get configuration: " + configPath);
			InputStream inputStream = new FileInputStream(configPath);
			p = new Properties();
			p.load(inputStream);

		} catch (Exception e) {
			logger.error("Error while loading " + file + e.getMessage());
		}
	}

	/**
	 * @param field
	 * @return
	 */
	public String getProperty(String field) {
		if (p == null) {
			logger.error("Properties have not been loaded");
			return null;
		} else {
			return p.getProperty(field);
		}
	}

	/**
	 * @param field
	 * @return
	 */
	public int getInt(String field) {
		if (p == null) {
			logger.error("Properties have not been loaded");
			return -1;
		} else {
			try {
				return Integer.parseInt(p.getProperty(field));
			} catch (Exception e) {
				logger.error("Error parsing while loading properties");
				return -1;
			}
		}
	}

	/**
	 * @param field
	 * @return
	 */
	public float getFloat(String field) {
		if (p == null) {
			logger.error("Properties have not been loaded");
			return -1;
		} else {
			try {
				return Float.parseFloat(p.getProperty(field));
			} catch (Exception e) {
				logger.error("Error parsing while loading properties");
				return -1;
			}
		}
	}

	/**
	 * @param field
	 * @return
	 */
	public boolean getBoolean(String field) {
		if (p == null) {
			logger.error("Properties have not been loaded");
			return false;
		} else {
			try {
				return Boolean.parseBoolean(p.getProperty(field));
			} catch (Exception e) {
				logger.error("Error parsing while loading properties");
				return false;
			}
		}
	}

	/**
	 * @return
	 */
	public Properties getProperties() {
		return p;
	}
}
