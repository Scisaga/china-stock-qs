package org.tfelab.proxy_hub.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.msg.*;
import org.tfelab.proxy_hub.server.ClientChannelMap;
import static org.tfelab.proxy_hub.msg.Msg.Type.*;

/**
 *
 */
public class ClientRegistrationHandler extends SimpleChannelInboundHandler<Msg> {

    public static final Logger logger = LogManager.getLogger(ClientRegistrationHandler.class.getName());

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientChannelMap.remove((SocketChannel)ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Msg msg) throws Exception {

        if(msg.getType() == Login){

            LoginMsg loginMsg = (LoginMsg) msg;

            if(loginMsg.getSecrets().equals(ProxyContext.Secrets)){

                ClientChannelMap.add(loginMsg.getClientId(), (SocketChannel) ctx.channel());
                logger.info("Client:{} login success. ", loginMsg.getClientId());
            }
        }
        else {

        	// 之前连接失效
            if(ClientChannelMap.get(msg.getClientId()) == null){
                AskMsg askMsg = new AskMsg(null, AskMsg.Type.RequireLogin);
				ctx.channel().writeAndFlush(askMsg);
            }

            if(msg.getType() == Ping) {
            	logger.info("Receive msg from client:{}. ", msg.getClientId());
				ClientChannelMap.get(msg.getClientId()).writeAndFlush(new PingMsg(null));
			}
			else if(msg.getType() == Ask) {
				AskMsg askMsg = (AskMsg) msg;
				if(true) { // TODO should verify AskMsg token here
					// 当前服务器端不接受客户端请求
				}
			}
			else if(msg.getType() == Reply) {

				ReplyMsg replyMsg = (ReplyMsg) msg;
				logger.info(replyMsg.getBody().toJSON());

			}
        }

        ReferenceCountUtil.release(msg);
    }
}
