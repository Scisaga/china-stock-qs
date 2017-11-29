package org.tfelab.proxy_hub.client;

import com.typesafe.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.Configs;
import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;
import org.tfelab.monitor.CPUInfo;
import org.tfelab.monitor.IoInfo;
import org.tfelab.monitor.MemInfo;
import org.tfelab.monitor.NetInfo;
import org.tfelab.monitor.sensors.LocalSensor;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.common.UserSecret;
import org.tfelab.proxy_hub.msg.LoginMsg;
import org.tfelab.proxy_hub.msg.Msg;
import org.tfelab.proxy_hub.msg.ReplyMsg;
import org.tfelab.stock_qs.model.Proxy;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.util.NetworkUtil;

import javax.net.ssl.SSLException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ProxyNode {

	public static final Logger logger = LogManager.getLogger(ProxyNode.class.getName());

	protected static ProxyNode instance;

	public static ProxyNode getInstance() {

		if (instance == null) {
			synchronized (ProxyManager.class) {
				if (instance == null) {
					instance = new ProxyNode();
				}
			}
		}

		return instance;
	}

	// id
	public String id;

	//
	private SslContext sslCtx;

	private SocketChannel channel;
	private Bootstrap bootstrap;

	/**
	 *
	 */
	public ProxyNode() {

		Config config = Configs.getConfig(ProxyContext.class);
		id = config.getString("client_id");
		try {
			sslCtx = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} catch (SSLException e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 */
	public void run() {

		EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

		bootstrap = new Bootstrap();
					bootstrap.group(eventLoopGroup)
							.channel(NioSocketChannel.class)
							.handler(new LoggingHandler(LogLevel.INFO))
							.option(ChannelOption.SO_KEEPALIVE, true)
							.remoteAddress(ProxyContext.host, ProxyContext.client_port)
							.handler(new ChannelInitializer<SocketChannel>() {
								@Override
								protected void initChannel(SocketChannel socketChannel) throws Exception {
									ChannelPipeline p = socketChannel.pipeline();
									p.addLast(new IdleStateHandler(0,0,60));
									p.addLast(sslCtx.newHandler(socketChannel.alloc(), ProxyContext.host, ProxyContext.client_port));
									p.addLast(new ObjectEncoder());
									p.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
									p.addLast(new ClientHandler());
								}
							});
		connect();
	}

	public void connect() {

		if (channel != null && channel.isActive()) {
			return;
		}


			ChannelFuture cf = bootstrap.connect(ProxyContext.host, ProxyContext.client_port);

			cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture cf) throws Exception {
					if(cf.isSuccess()) {

						channel = (SocketChannel) cf.channel();
						logger.info("Connect to registration server: {}:{}. ", ProxyContext.host, ProxyContext.client_port);

						channel.closeFuture().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								logger.info("Lost server connection. ");
								connect();
							}
						});

						LoginMsg loginMsg = new LoginMsg(id);
						loginMsg.setSecrets(ProxyContext.secrets);
						channel.writeAndFlush(loginMsg);

						logger.info("Send login request to server. ");
					}
					else {
						cf.channel().close();
						logger.warn("Failed to connect to registration server: {}:{}. ", ProxyContext.host, ProxyContext.client_port);

						cf.channel().eventLoop().schedule(new Runnable() {
							@Override
							public void run() {
								connect();
							}
						}, 10, TimeUnit.SECONDS);
					}
				}
			});
	}

	/**
	 *
	 * @return
	 */
	public String getLocalIp() {
		return NetworkUtil.getLocalIp();
	}

	/**
	 *
	 * @return
	 */
	public NodeInfo getNodeInfo(String ip) {
		LocalSensor<CPUInfo> sensor1 = new LocalSensor<>();
		LocalSensor<IoInfo> sensor2 = new LocalSensor<>();
		LocalSensor<MemInfo> sensor3 = new LocalSensor<>();
		LocalSensor<NetInfo> sensor4 = new LocalSensor<>();
		NodeInfo info = new NodeInfo(
				ip,
				sensor1.get(new CPUInfo()),
				sensor2.get(new IoInfo()),
				sensor3.get(new MemInfo()),
				sensor4.get(new NetInfo()));
		return info;
	}

	public static class NodeInfo implements JSONable, Serializable {

		private static final long serialVersionUID = 1L;

		public String current_ip = "";
		public float cpu_usage = 0;
		public float io_read = 0;
		public float io_writen = 0;
		public float mem_total = 0;
		public float mem_avail = 0;
		public double net_in_rate = 0;
		public double net_out_rate = 0;

		public NodeInfo(String ip, CPUInfo ci, IoInfo ii, MemInfo mi, NetInfo ni){
			current_ip = ip;
			cpu_usage = ci.usage;
			io_read = ii.read;
			io_writen = ii.writen;
			mem_total = mi.total;
			mem_avail = mi.avail;
			net_in_rate = ni.in_rate;
			net_out_rate = ni.out_rate;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	public void sendMsgToServer(Msg msg) {
		channel.writeAndFlush(msg);
	}

	public String pon(UserSecret userSecret) {
		PPPoEUtil.addUserSecret("11111102664885", "ezkzct");
		PPPoEUtil.createProviderConifg("11111102664885");
		PPPoEUtil.poff();
		//PPPoEUtil.changeMACAddress();
		return PPPoEUtil.pon();
	}

	public static void main(String[] args) throws Exception {

		ProxyNode.getInstance().run();
	}
}
