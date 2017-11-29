package org.tfelab.proxy_hub.client;

import com.typesafe.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.Configs;
import org.tfelab.io.requester.proxy.IpDetector;
import org.tfelab.monitor.IoInfo;
import org.tfelab.monitor.sensors.LocalSensor;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.server.handler.ClientRegistrationHandler;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.util.NetworkUtil;

import javax.net.ssl.SSLException;

public class ProxyNode {

	public static final Logger logger = LogManager.getLogger(ProxyNode.class.getName());

	protected static ProxyNode instance;

	public static ProxyNode getInstance() {

		if (instance == null) {
			synchronized (ProxyManager.class) {
				if (instance == null) {
					instance = new ProxyNode();
				}
			}
		}

		return instance;
	}

	// id
	public String id;

	//
	private SslContext sslCtx;

	/**
	 *
	 */
	public ProxyNode() {

		Config config = Configs.getConfig(ProxyContext.class);
		id = config.getString("client_id");
		try {
			sslCtx = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} catch (SSLException e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 */
	public void run() {

		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

		new Thread() {
			public void run() {
				try {
					Bootstrap bootstrap = new Bootstrap();
					bootstrap.group(eventLoopGroup)
							.channel(NioSocketChannel.class)
							.handler(new LoggingHandler(LogLevel.INFO))
							.option(ChannelOption.SO_KEEPALIVE, true)
							.remoteAddress(ProxyContext.host, ProxyContext.port)
							.handler(new ChannelInitializer<SocketChannel>() {
								@Override
								protected void initChannel(SocketChannel socketChannel) throws Exception {
									ChannelPipeline p = socketChannel.pipeline();
									p.addLast(sslCtx.newHandler(socketChannel.alloc(), ProxyContext.host, ProxyContext.port));
									p.addLast(new ObjectEncoder());
									p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
									p.addLast(new ClientRegistrationHandler());
								}
							});

					ChannelFuture cf = bootstrap.connect(ProxyContext.host, ProxyContext.port).sync();

					if(cf.isSuccess()) {
						logger.info("Client: {} connect to registration server: {}:{}. ", id, ProxyContext.host, ProxyContext.port);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}.start();

	}

	/**
	 *
	 * @return
	 */
	public String getLocalIp() {
		return IpDetector.getIp() + " :: " + NetworkUtil.getLocalIp();
	}


	public void getSysInfo() {
		LocalSensor<IoInfo> sensor = new LocalSensor<>();
		System.out.println(sensor.get(new IoInfo()).toJSON());
	}

}
