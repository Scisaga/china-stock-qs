package org.tfelab.proxy_hub.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
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
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.proxy_hub.server.handler.ClientRegistrationHandler;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * Proxy Hub Server
 * @author scisaga@gmail.com
 * @date 2017/11/28
 */
public class Server {

	public static final Logger logger = LogManager.getLogger(Server.class.getName());

	public int proxy_port = 50101;
	public int client_port = 50102; // 客户端反向连接端口

	SelfSignedCertificate ssc;
	SslContext sslCtx;

	public Server() {

		SelfSignedCertificate ssc = null;
		try {
			ssc = new SelfSignedCertificate();
			SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		EventLoopGroup workerGroup_client = new NioEventLoopGroup();

		new Thread() {
			public void run() {
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

					ChannelFuture cf = bootstrap.bind(client_port).sync();

					if(cf.isSuccess()) {
						logger.info("Client registration server start @port: {}. ", client_port);
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}.start();

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
		new Server().run();
	}
}
