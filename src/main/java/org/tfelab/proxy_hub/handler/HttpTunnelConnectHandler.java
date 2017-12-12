package org.tfelab.proxy_hub.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpTunnelConnectHandler extends SimpleChannelInboundHandler<HttpRequest>
		implements TcpProxyHandlerTraits {

	private static final Logger logger = LoggerFactory.getLogger(HttpProxyConnectHandler.class);


	public HttpProxyConnectHandler(@Nullable MessageListener messageListener,
								   @Nullable SSLContextManager sslContextManager,
								   @Nullable Supplier<ProxyHandler> proxyHandlerSupplier) {
		this.messageListener = messageListener;
		this.sslContextManager = sslContextManager;
		this.proxyHandlerSupplier = proxyHandlerSupplier;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
		Promise<Channel> promise = ctx.executor().newPromise();
		Bootstrap bootstrap = initBootStrap(promise, ctx.channel().eventLoop());

		NetAddress address = Networks.parseAddress(request.uri());
		bootstrap.connect(address.getHost(), address.getPort()).addListener((ChannelFutureListener) future -> {
			if (!future.isSuccess()) {
				ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
				NettyUtils.closeOnFlush(ctx.channel());
			}
		});

		promise.addListener((FutureListener<Channel>) future -> {
			if (!future.isSuccess()) {
				ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_GATEWAY));
				NettyUtils.closeOnFlush(ctx.channel());
				return;
			}

			Channel outboundChannel = future.getNow();
			ChannelFuture responseFuture = ctx.channel().writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, OK));
			responseFuture.addListener((ChannelFutureListener) channelFuture -> {
				ctx.pipeline().remove(HttpProxyConnectHandler.this);
				ctx.pipeline().remove(HttpServerCodec.class);
				initTcpProxyHandlers(ctx, address, outboundChannel);
			});
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
		logger.error("", e);
		NettyUtils.closeOnFlush(ctx.channel());
	}

	@Nullable
	@Override
	public MessageListener messageListener() {
		return messageListener;
	}

	@Nullable
	@Override
	public SSLContextManager sslContextManager() {
		return sslContextManager;
	}

	@Nullable
	@Override
	public Supplier<ProxyHandler> proxyHandlerSupplier() {
		return proxyHandlerSupplier;
	}
}