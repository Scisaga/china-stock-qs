package org.tfelab.proxy_hub.server;

import com.mongodb.gridfs.CLI;
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
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.common.UserSecret;
import org.tfelab.proxy_hub.msg.AskMsg;
import org.tfelab.proxy_hub.server.handler.ClientRegistrationHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Proxy Hub ProxyServer
 * @author scisaga@gmail.com
 * @date 2017/11/28
 */
public class ProxyServer {

	public static final Logger logger = LogManager.getLogger(ProxyServer.class.getName());

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

	public void run() {

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		EventLoopGroup workerGroup_client = new NioEventLoopGroup();

				try {
					ServerBootstrap bootstrap = new ServerBootstrap();
					bootstrap.group(workerGroup_client, workerGroup_client)
							.channel(NioServerSocketChannel.class)
							.handler(new LoggingHandler(LogLevel.INFO))
							.option(ChannelOption.SO_BACKLOG, 128)
							.option(ChannelOption.TCP_NODELAY, true)
							.childOption(ChannelOption.SO_KEEPALIVE, true)
							.childHandler(new ChannelInitializer<SocketChannel>() {
								@Override
								protected void initChannel(SocketChannel socketChannel) throws Exception {
									ChannelPipeline p = socketChannel.pipeline();

/*									SSLEngine sslEngine = sslCtx.newEngine(UnpooledByteBufAllocator.DEFAULT); // 创建一个新的 SSLEngine 对象
									sslEngine.setUseClientMode(false); // 配置为 server 模式
									sslEngine.setEnabledProtocols(new String[] {"TLSv1.2"}); // 选择需要启用的 SSL 协议，如 SSLv2 SSLv3 TLSv1 TLSv1.1 TLSv1.2 等
									sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites()); // 选择需要启用的 CipherSuite 组合，如 ECDHE-ECDSA-CHACHA20-POLY1305 等*/

									/*p.addLast("ssl", new SslHandler(sslEngine)); // 添加 SslHandler 之 pipeline 中*/
									p.addLast(sslCtx.newHandler(socketChannel.alloc()));
									p.addLast(new ObjectEncoder());
									p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
									p.addLast(new ClientRegistrationHandler());
								}
							});

					ChannelFuture cf = bootstrap.bind(ProxyContext.client_port).sync();

					if(cf.isSuccess()) {
						logger.info("Registration server start @port: {}. ", ProxyContext.client_port);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				for(String key : ClientChannelMap.getKeys()) {

					Channel channel = ClientChannelMap.get(key);
					if(channel.isActive()) {
						AskMsg askMsg = new AskMsg(null, AskMsg.Type.Pon);
						askMsg.setBody(new UserSecret("11111102664885", "ezkzct"));
						ClientChannelMap.get(key).writeAndFlush(askMsg);
					} else {
						channel.close();
						ClientChannelMap.add(key, null);
						// TODO should remove client key and channel
					}
				}

			}
		}, 10000, 20000);

//		new Thread() {
//			public void run() {
//				try {
//					ServerBootstrap b = new ServerBootstrap();
//					b.group(bossGroup, workerGroup)
//							.channel(NioServerSocketChannel.class)
//							.handler(new LoggingHandler(LogLevel.INFO))
//							.childHandler(new SocksServerInitializer());
//					b.bind(proxy_port).sync().channel().closeFuture().sync();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} finally {
//					bossGroup.shutdownGracefully();
//					workerGroup.shutdownGracefully();
//				}
//			}
//		}.start();
	}

	public static void main(String[] args) throws Exception {
		new ProxyServer().run();
	}
}
