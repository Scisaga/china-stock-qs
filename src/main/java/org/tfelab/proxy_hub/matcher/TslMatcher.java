package org.tfelab.proxy_hub.matcher;

import io.netty.buffer.ByteBuf;

public class TslMatcher extends ProtocolMatcher {

	@Override
	public int match(ByteBuf buf) {
		if (buf.readableBytes() < 3) {
			return PENDING;
		}
		byte first = buf.getByte(buf.readerIndex());
		byte second = buf.getByte(buf.readerIndex() + 1);
		byte third = buf.getByte(buf.readerIndex() + 2);
		if (first == 22 && second <= 3 && third <= 3) {
			return MATCH;
		}
		return DISMATCH;
	}
}
