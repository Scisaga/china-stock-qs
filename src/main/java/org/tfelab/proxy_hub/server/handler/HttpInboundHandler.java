package org.tfelab.proxy_hub.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.regex.Pattern;

public class HttpInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final Pattern Path_Pattern = Pattern.compile("(https?)://([a-zA-Z0-9\\.\\-]+)(:(\\d+))?(/.*)");
	private static final Pattern Tunnel_Addr_Pattern = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+):(\\d+)");

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

		/**
		 * HTTP 隧道请求
		 */
		if(request.method() == HttpMethod.CONNECT) {

		}
	}
}
