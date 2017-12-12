package org.tfelab.proxy_hub.matcher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;
import java.util.function.Consumer;

/**
 * Matcher for protocol.
 */
public abstract class ProtocolMatcher {

	public static int MATCH = 1;
	public static int DISMATCH = -1;
	public static int PENDING = 0;

	/**
	 * 在引用时定义
	 */
	private Consumer<ChannelPipeline> handler = p -> {};

	/**
	 * If match the protocol.
	 *
	 * @return 1:match, -1:not match, 0:still can not judge now
	 */
	public abstract int match(ByteBuf buf);

	/**
	 * 在定义时，使用lambda表达式定义handler
	 * matcher 在匹配成功后调用 handlePipeline 就可以使用定义好的handler对 pipeline进行操作
	 * @param handler
	 * @return
	 */
	public ProtocolMatcher onMatched(Consumer<ChannelPipeline> handler) {
		this.handler = handler;
		return this;
	}

	/**
	 * Deal with the pipeline when matched
	 */
	public void handlePipeline(ChannelPipeline pipeline) {
		handler.accept(pipeline);
	}
}
