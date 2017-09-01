package org.tfelab.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * 
 * @author karajan@tfelab.org
 * 2016年4月28日 上午12:42:18
 */
public class NetworkUtil {
	
	private static String ipPattern = "(\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})";
	
	/**
	 * 获取本地IP地址
	 * @return
	 */
//	public static InetAddress getLocalInetAddress() {
//
//		Enumeration<?> item;
//		try {
//			
//			item = NetworkInterface.getNetworkInterfaces();
//			while (item.hasMoreElements()) {
//
//				NetworkInterface n = (NetworkInterface) item.nextElement();
//				Enumeration<?> ee = n.getInetAddresses();
//				while (ee.hasMoreElements()) {
//
//					InetAddress i = (InetAddress) ee.nextElement();
//					
//					if (i.getHostAddress().matches(ipPattern) && !i.getHostAddress().equals("127.0.0.1")) {
//						return i;
//					}
//				}
//			}
//
//			return InetAddress.getLocalHost();
//			
//		} catch (SocketException | UnknownHostException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	/**
	 * 获取本机所有IP地址字串
	 * @return
	 */
	public static String getAllInetIpString() {
		
		String ipString = "";

		Enumeration<?> item;
		try {
			
			item = NetworkInterface.getNetworkInterfaces();
			while (item.hasMoreElements()) {

				NetworkInterface n = (NetworkInterface) item.nextElement();
				Enumeration<?> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {

					InetAddress i = (InetAddress) ee.nextElement();
					
					if (i.getHostAddress().matches(ipPattern) && !i.getHostAddress().equals("127.0.0.1")) {
						ipString += i.getHostAddress() + "-";
					}
				}
			}
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return ipString;
	}
	
	/**
	 * 获取本地IP地址
	 * @return
	 */
	public static String getLocalIp() {

		String localIp = "";

		Enumeration<?> item;
		
		try {
			item = NetworkInterface.getNetworkInterfaces();
			while (item.hasMoreElements()) {

				NetworkInterface n = (NetworkInterface) item.nextElement();
				Enumeration<?> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {

					InetAddress i = (InetAddress) ee.nextElement();
					
					if (i.getHostAddress().matches(ipPattern) && !i.getHostAddress().equals("127.0.0.1")) {
						localIp = i.getHostAddress();
						break;
					}
				}
			}

			if (localIp.equals("")) {
				localIp = InetAddress.getLocalHost().getHostAddress().toString();
			}
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
		return localIp;
	}
	
}
