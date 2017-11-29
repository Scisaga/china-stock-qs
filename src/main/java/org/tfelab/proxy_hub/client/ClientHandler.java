package org.tfelab.proxy_hub.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.io.requester.proxy.IpDetector;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.common.UserSecret;
import org.tfelab.proxy_hub.msg.*;

import static org.tfelab.proxy_hub.msg.Msg.Type.*;
import static org.tfelab.proxy_hub.msg.AskMsg.Type.*;

public class ClientHandler extends SimpleChannelInboundHandler<Msg> {

	public static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());

	@Override
	/**
	 * 心跳包实现
	 */
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			switch (e.state()) {
				case ALL_IDLE:
					PingMsg pingMsg = new PingMsg(ProxyNode.getInstance().id);
					ctx.writeAndFlush(pingMsg);
					break;
				default:
					break;
			}
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Msg msg) throws Exception {

		if(msg.getType() == Ask) {
			AskMsg askMsg = (AskMsg) msg;
			if(askMsg.getAskType() == RequireLogin) {
				LoginMsg loginMsg = new LoginMsg(ProxyNode.getInstance().id);
				loginMsg.setSecrets(ProxyContext.secrets);
				ctx.writeAndFlush(loginMsg);
			}
			else if (askMsg.getAskType() == NodeInfo) {
				ReplyMsg replyMsg = new ReplyMsg(ProxyNode.getInstance().id);
				replyMsg.setBody(ProxyNode.getInstance().getNodeInfo(IpDetector.getIp()));
				ctx.writeAndFlush(replyMsg);
			}
			else if (askMsg.getAskType() == Pon) {

				UserSecret userSecret = (UserSecret) askMsg.getBody();
				String ip = ProxyNode.getInstance().pon(userSecret);

				ReplyMsg replyMsg = new ReplyMsg(ProxyNode.getInstance().id);
				replyMsg.setBody(ProxyNode.getInstance().getNodeInfo(ip));
				ctx.writeAndFlush(replyMsg);
			}
		}
		else if(msg.getType() == Ping) {
			logger.info("Received pin msg from server.");
		}
		else if(msg.getType() == Reply) {

			ReplyMsg replyMsg = (ReplyMsg) msg;
			logger.info(replyMsg.getBody());

		}
	}
}
