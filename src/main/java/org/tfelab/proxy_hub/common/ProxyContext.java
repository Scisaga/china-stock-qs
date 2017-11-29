package org.tfelab.proxy_hub.common;

import com.typesafe.config.Config;
import org.tfelab.common.Configs;

public class ProxyContext {

	public static String secrets;

	public static String host;
	public static int client_port;

	static {
		// read config
		Config config = Configs.getConfig(ProxyContext.class);
		secrets = config.getString("secrets");
		host = config.getString("host");
		client_port = config.getInt("client_port");
	}

}
