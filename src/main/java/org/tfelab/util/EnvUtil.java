package org.tfelab.util;

public class EnvUtil {
	
	public static boolean isHostLinux() {
		String os = System.getProperty("os.name");
		if (os != null && os.toLowerCase().startsWith("linux")) {
			return true;
		}
		return false;
	}
}
