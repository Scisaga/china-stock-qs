package org.tfelab.proxy_hub.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.proxy_hub.common.Address;
import org.tfelab.proxy_hub.common.ConnectionInfo;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.common.UserSecret;
import org.tfelab.proxy_hub.msg.AskMsg;
import org.tfelab.proxy_hub.server.handler.ClientRegistrationHandler;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Proxy Hub ProxyServer
 * @author scisaga@gmail.com
 * @date 2017/11/28
 */
public class ProxyServer {

	public static final Logger logger = LogManager.getLogger(ProxyServer.class.getName());

	EventLoopGroup bossGroup;
	EventLoopGroup bossGroup_client;
	EventLoopGroup workerGroup;
	EventLoopGroup workerGroup_client;

	SelfSignedCertificate ssc;
	SslContext sslCtx;

	Timer timer = new Timer();


	public ProxyServer() {

		SelfSignedCertificate ssc = null;
		try {
			ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 */
	public void run() {

		bossGroup = new NioEventLoopGroup(1);
		bossGroup_client = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
		workerGroup_client = new NioEventLoopGroup();

		/**
		 * A
		 */
		try {

			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup_client, workerGroup_client)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO))
					.option(ChannelOption.SO_BACKLOG, 128)
					.option(ChannelOption.TCP_NODELAY, true)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();

							/*SSLEngine sslEngine = sslCtx.newEngine(UnpooledByteBufAllocator.DEFAULT); // 创建一个新的 SSLEngine 对象
							sslEngine.setUseClientMode(false); // 配置为 server 模式
							sslEngine.setEnabledProtocols(new String[] {"TLSv1.2"}); // 选择需要启用的 SSL 协议，如 SSLv2 SSLv3 TLSv1 TLSv1.1 TLSv1.2 等
							sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites()); // 选择需要启用的 CipherSuite 组合，如 ECDHE-ECDSA-CHACHA20-POLY1305 等
							p.addLast("ssl", new SslHandler(sslEngine)); // 添加 SslHandler 之 pipeline 中*/
							p.addLast(sslCtx.newHandler(ch.alloc()));
							p.addLast(new ObjectEncoder());
							p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
							p.addLast(new ClientRegistrationHandler());
						}
					});

			ChannelFuture cf = bootstrap.bind(ProxyContext.Client_Port).sync();

			if(cf.isSuccess()) {
				logger.info("Registration server start @port: {}. ", ProxyContext.Client_Port);
			}

		} catch (InterruptedException e) {
			logger.error(e);
		} finally {
			bossGroup_client.shutdownGracefully();
			workerGroup_client.shutdownGracefully();
		}

		/**
		 * B
		 */
		try {

			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel ch) throws Exception {

							ChannelPipeline p = ch.pipeline();
							InetSocketAddress address = (InetSocketAddress) ch.remoteAddress();
							Address clientAddress = new Address(address.getHostName(), address.getPort());
							ConnectionInfo connectionInfo = new ConnectionInfo(clientAddress);

						}
					});

			ChannelFuture cf = bootstrap
					.bind(ProxyContext.Proxy_Port)
					.sync();

			if(cf.isSuccess()) {
				logger.info("Proxy server start @port: {}. ", ProxyContext.Proxy_Port);
			}

		} catch (InterruptedException e) {
			logger.error(e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 *
	 */
	public void runTimerTask() {

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				for(String key : ClientChannelMap.getKeys()) {

					Channel channel = ClientChannelMap.get(key);
					if(channel != null && channel.isActive()) {
						AskMsg askMsg = new AskMsg(null, AskMsg.Type.Pon);
						askMsg.setBody(new UserSecret("11111102664885", "ezkzct"));
						ClientChannelMap.get(key).writeAndFlush(askMsg);
					} else {
						channel.close();
						ClientChannelMap.remove(key);
					}
				}

			}
		}, 10000, 20000);
	}

	public static void main(String[] args) throws Exception {
		new ProxyServer().run();
	}
}
