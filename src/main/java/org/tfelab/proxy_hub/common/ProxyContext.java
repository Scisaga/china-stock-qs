package org.tfelab.proxy_hub.common;

import com.typesafe.config.Config;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.tfelab.common.Configs;

public class ProxyContext {

	public static String Secrets;

	public static String Host;
	public static int Client_Port;
	public static int Proxy_Port;
	public static int Connect_Timeout = 30000;

	public static final int rootCertValidityDays = 3650;
	public static final int certValidityDays = 10;


	public static final String rootKeyStorePath = "./certs/root.jks";
	public static final char[] rootCertPassword = "TfeLAB".toCharArray();
	public static final char[] rootKeyStorePassword = "TfeLAB".toCharArray();
	public static final char[] certPassword = "TfeLAB".toCharArray();
	public static final char[] keyStorePassword = "TfeLAB".toCharArray();

	static {
		// read config
		Config config = Configs.getConfig(ProxyContext.class);
		Secrets = config.getString("secrets");
		Host = config.getString("host");
		Client_Port = config.getInt("client_port");
		Proxy_Port = config.getInt("proxy_port");
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	public static void closeOnFlush(Channel channel) {
		if (channel.isActive()) {
			channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}
