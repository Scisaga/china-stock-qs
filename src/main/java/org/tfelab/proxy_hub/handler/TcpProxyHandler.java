package org.tfelab.proxy_hub.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrameEncoder;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;
import org.tfelab.proxy_hub.common.Address;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.matcher.AnyMatcher;
import org.tfelab.proxy_hub.matcher.TslMatcher;
import org.tfelab.proxy_hub.tls.ClientSSLContextFactory;
import org.tfelab.proxy_hub.tls.SSLContextManager;

import javax.net.ssl.SSLEngine;
import java.util.function.Supplier;

public interface TcpProxyHandler {

	default Bootstrap initBootStrap(Promise<Channel> promise, EventLoopGroup eventLoopGroup) {
		return new Bootstrap()
				.group(eventLoopGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ProxyContext.Connect_Timeout)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new ChannelActiveAwareHandler(promise));
					}
				});
	}

	default void initTcpProxyHandlers(ChannelHandlerContext ctx, Address address, Channel outboundChannel) {
		boolean intercept = false;
		if (!intercept) {
			ctx.pipeline().addLast(new ReplayHandler(outboundChannel));
			outboundChannel.pipeline().addLast(new ReplayHandler(ctx.channel()));
			return;
		}

		SSLContextManager sslContextManager = sslContextManager();
		if (sslContextManager == null) {
			throw new RuntimeException("SSLContextManager must be set when use mitm. ");
		}

		ProtocolDetector protocolDetector = new ProtocolDetector(
				new TslMatcher().onMatched(p -> {
					//TODO: create ssl context is slow, should execute in another executor?
					SSLEngine serverSSLEngine = sslContextManager.createSSlContext(address.getHost()).createSSLEngine();
					serverSSLEngine.setUseClientMode(false);
					p.addLast("ssl", new SslHandler(serverSSLEngine));

					SSLEngine sslEngine = ClientSSLContextFactory.getInstance().get()
							.createSSLEngine(address.getHost(), address.getPort()); // using SNI
					sslEngine.setUseClientMode(true);
					outboundChannel.pipeline().addLast(new SslHandler(sslEngine));
					initPlainHandler(ctx, address, outboundChannel, true);
				}),
				new AnyMatcher().onMatched(p -> initPlainHandler(ctx, address, outboundChannel, false))
		);
		ctx.pipeline().addLast(protocolDetector);
	}


	default void initPlainHandler(ChannelHandlerContext ctx, NetAddress address, Channel outboundChannel, boolean ssl) {

		ctx.pipeline().addLast(new HttpServerCodec());
		ctx.pipeline().addLast("", new HttpServerExpectContinueHandler());
		ctx.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(outboundChannel));

		outboundChannel.pipeline().addLast(new HttpClientCodec());
		outboundChannel.pipeline().addLast("tcp-tunnel-handler", new ReplayHandler(ctx.channel()));

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
