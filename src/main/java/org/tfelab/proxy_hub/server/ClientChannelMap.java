package org.tfelab.proxy_hub.server;

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端Channel缓存
 * @author scisaga@gmail.com
 * @date 2017/11/28
 */
public class ClientChannelMap {

	private static Map<String, SocketChannel> map = new ConcurrentHashMap<String, SocketChannel>();

	public static void add(String clientId, SocketChannel socketChannel){
		map.put(clientId,socketChannel);
	}

	public static Channel get(String clientId){
		return map.get(clientId);
	}

	public static void remove(String clientId){
		map.remove(clientId);
	}

	public static void remove(SocketChannel socketChannel){
		for (Map.Entry entry : map.entrySet()){
			if (entry.getValue() == socketChannel){
				map.remove(entry.getKey());
			}
		}
	}

	public static Set<String> getKeys() {
		return map.keySet();
	}
}
