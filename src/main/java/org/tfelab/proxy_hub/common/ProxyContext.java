package org.tfelab.proxy_hub.common;

import com.typesafe.config.Config;
import org.tfelab.common.Configs;

public class ProxyContext {

	public static String secrets;

	public static String host;
	public static int port;

	static {
		// read config
		Config config = Configs.getConfig(ProxyContext.class);
		secrets = config.getString("secrets");
		host = config.getString("host");
		port = config.getInt("port");
	}

}
