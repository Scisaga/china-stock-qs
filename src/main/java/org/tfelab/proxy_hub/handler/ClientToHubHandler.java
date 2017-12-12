package org.tfelab.proxy_hub.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.proxy_hub.common.ProxyContext;

import static io.netty.handler.codec.ByteToMessageDecoder.MERGE_CUMULATOR;

public class ClientToHubHandler extends ChannelInboundHandlerAdapter {

	public static final Logger logger = LogManager.getLogger(ClientToHubHandler.class.getName());

	private State state = State.CONNECTING;
	private final ByteToMessageDecoder.Cumulator cumulator = MERGE_CUMULATOR;
	private int index;
	private ByteBuf buf;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		if (!(msg instanceof ByteBuf)) {
			logger.error("unexpected message type for ProtocolDetector: {}", msg.getClass());
			ProxyContext.closeOnFlush(ctx.channel());
			return;
		}

		ByteBuf in = (ByteBuf) msg;
		if (buf == null) {
			buf = in;
		} else {
			buf = cumulator.cumulate(ctx.alloc(), buf, in);
		}

		for (int i = index; i < matcherList.length; i++) {
			ProtocolMatcher matcher = matcherList[i];
			int match = matcher.match(buf.duplicate());
			if (match == ProtocolMatcher.MATCH) {

				logger.debug("matched by {}", matcher.getClass().getName());
				matcher.handlePipeline(ctx.pipeline());
				ctx.pipeline().remove(this);
				ctx.fireChannelRead(buf);
				buf = null;
				return;
			}

			if (match == ProtocolMatcher.PENDING) {
				index = i;
				return;
			}
		}

		// Miss all Detectors
		logger.error("unsupported protocol");
		buf.release();
		buf = null;
		ProxyContext.closeOnFlush(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (buf != null) {
			buf.release();
			buf = null;
		}
		logger.error("", cause);
		ProxyContext.closeOnFlush(ctx.channel());
	}

	public enum State {
		AWAITING_INITIAL,
		CONNECTING,
		Connection_Established,
		Protocol_Matched
	}

}
