package org.tfelab.proxy_hub.matcher;

import io.netty.buffer.ByteBuf;

/**
 * 匹配任意的Protocol
 */
public class AnyMatcher extends ProtocolMatcher {

	public AnyMatcher() {
	}

	@Override
	public int match(ByteBuf buf) {
		return MATCH;
	}
}
