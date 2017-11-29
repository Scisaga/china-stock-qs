package org.tfelab.stock_qs.proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.io.requester.BasicRequester.ConnectionBuilder;
import org.tfelab.io.requester.proxy.IpDetector;
import org.tfelab.io.requester.proxy.ProxyWrapper;
import org.tfelab.io.requester.proxy.ProxyWrapperImpl;
import org.tfelab.stock_qs.Crawler;

public class ProxyValidator {
	
	protected static ProxyValidator instance;
	
	public static ProxyValidator getInstance() {
		
		if (instance == null) {
			synchronized (ProxyValidator.class) {
				if (instance == null) {
					instance = new ProxyValidator();
				}
			}
		}

		return instance;
	}
	
	private static final String BAIDU = "http://www.baidu.com/";
	private static final String BAIDUS = "https://www.baidu.com/";
	private static final String GOOGLE = "http://www.google.com/";
	
	private static final Logger logger = LogManager.getLogger(ProxyValidator.class.getName());
	
	/**
	 * 测试结果类型
	 * @author karajan@tfelab.org
	 * 2017年3月15日 上午5:37:32
	 */
	public static enum Status {
		OK, Anonymous, Https, GL
	}
	
	/**
	 * 测试类型
	 * @author karajan@tfelab.org
	 * 2017年3月15日 上午5:37:50
	 */
	public static enum Type {
		TestAll, TestAlive
	}
	
	public ProxyValidator() {}

	/**
	 * 测试任务
	 * @author karajan@tfelab.org
	 * 2017年3月15日 上午8:31:12
	 */
	public class Task implements Runnable {
		
		ProxyWrapper pw;
		private Type type;
		private String proxyIp;
		private String host;
		public List<Status> status = new ArrayList<Status>();
		public long start_ts;
		public long duration = 0;
		public double speed = 0;
		private String url;
		
		/**
		 * 
		 * @param type
		 * @param pw
		 * @param url
		 * @param host
		 */
		public Task(Type type, ProxyWrapper pw, String url, String host) {
			this.type = type;
			this.pw = pw;
			this.host = host;
			this.url = url;
		}
		
		@Override
		public void run() {
			
			this.start_ts = System.nanoTime();
			
			int code = Integer.MAX_VALUE;
			HttpURLConnection conn;
			
			/**
			 * 简单测试代理是否可以使用
			 */
			try {
				
				logger.info("Test Alive --> {}", pw.getInfo());
				
				if(url == null) url = BAIDU;
				
				conn = new ConnectionBuilder(url, pw).build();
				code = conn.getResponseCode();
				
				if(code < 400) {
					if(url.equals(BAIDU)) {
						
						if(checkContent(conn, "home.baidu.com")) {
							status.add(Status.OK);
						} else {
							return;
						}
					} 
					else {
						if(checkContent(conn, null)) {
							status.add(Status.OK);
						} else {
							return;
						}
					}
					
				} else {
					logger.info("Return Code: {}", code);
					return;
				}
			}
			catch (Exception e){
				logger.error("Error reach {}, {}", BAIDU, e.toString());
				return;
			}
			
			long length = getContentLength(conn);
			this.duration = System.nanoTime() - this.start_ts;
			if(length > 0) {
				this.speed = Math.pow(10, 9) * length / this.duration;
			}
			this.duration = (long) Math.ceil(this.duration / Math.pow(10, 6));
			
			/**
			 * 进一步测试代理的其他指标
			 */
			if(type == Type.TestAll) {
				
				/**
				 * 测试代理的匿名性
				 */
				try{
					logger.info("Test Anonymous --> {}", pw.getInfo());
					
					proxyIp = IpDetector.getIp(pw);
					if (proxyIp != null && host != null && !host.equals(proxyIp)) {
						status.add(Status.Anonymous);
					}
				}
				catch (Exception e){
					logger.error("Error get IP, {}", e.toString());
				}	
				
				/**
				 * 测试代理是否支持HTTPS请求
				 */
				try{
					logger.info("Test Https --> {}", pw.getInfo());
					
					conn = new ConnectionBuilder(BAIDUS, pw).build();
					
					code = ((HttpsURLConnection) conn).getResponseCode();
					
					if(code < 400 && checkContent(conn, "home.baidu.com")) {
						status.add(Status.Https);
					}
				}
				catch (Exception e){
					logger.error("Error reach {}, {}", BAIDUS, e.toString());
				}
				
				/**
				 * 测试代理是否可以翻墙
				 */
				try{
					logger.info("Test GL --> {}", pw.getInfo());
					
					conn = new ConnectionBuilder(GOOGLE, pw).build();
					
					code = conn.getResponseCode();
					if(code < 400 && checkContent(conn, "window.google")) {
						status.add(Status.GL);
					}	
				}
				catch (Exception e){
					logger.error("Error reach {}, {}", GOOGLE, e.toString());
				}
				
			}
		}
		
	}
	
	private static long getContentLength(HttpURLConnection conn){
		long length = 0;
		for(String key: conn.getHeaderFields().keySet()){
			if(key != null && key.equals("Content-Length")){
				length = Long.parseLong(conn.getHeaderFields().get(key).get(0));
			}
		}
		return length;
	}
	
	/**
	 * 验证HTTP请求返回的body长度与header字段中声明的一致
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	private static boolean checkContent(HttpURLConnection conn, String check_str) throws IOException{
		
		long length = getContentLength(conn);
		
		BufferedInputStream inStream = new BufferedInputStream(conn.getInputStream());
		
		byte[] buf = new byte[1024];
		ByteArrayOutputStream bOutStream = new ByteArrayOutputStream();

		int size = 0;
		while ((size = inStream.read(buf)) > 0) {
			bOutStream.write(buf, 0, size);
		}

		byte[] src = bOutStream.toByteArray();
		
		String text = new String(src, "UTF-8");

		bOutStream.close();
		inStream.close();
		
		logger.trace("{} --> {}", length, src.length);
		
		if(src.length >= length && src.length > 0) {
			
			if(check_str != null) {
				
				if(text.contains(check_str)) {
					return true;
				} else {
					logger.trace(text);
					return false;
				}
				
			} else {
				return true;
			}
			
		} else {
			logger.trace(text);
			return false;
		}
	}
	
	/**
	 * 
	 * @param pw
	 * @param type
	 * @param url
	 * @return
	 */
	public Task validate(ProxyWrapper pw, Type type, String url) {
		
		Task task = new Task(type, pw, url, Crawler.LOCAL_IP);
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<?> future = executor.submit(task);
		executor.shutdown();
		
		try {
			future.get(60, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			future.cancel(true);
		}
		if (!executor.isTerminated()){
			executor.shutdownNow();
		}
		
		if(task.status.contains(Status.Anonymous)) {
			logger.info("{} ---> {}", task.host, task.proxyIp);
		}
		
		return task;
	}
	
	/**
	 * 
	 * @param pw
	 * @param url
	 * @return
	 */
	public boolean isAlive(ProxyWrapper pw, String url) {
		
		Task task = this.validate(pw, Type.TestAlive, url);
		
		if(task.status.contains(Status.OK)) {
			logger.info("Proxy:{} Alive", pw.getInfo());
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String[] args) {
		ProxyWrapper pw = new ProxyWrapperImpl("10.0.0.18", 60103, "tfelab", "TfeLAB2@15");
		Task task = ProxyValidator.getInstance().validate(pw, Type.TestAlive, null);
		System.err.println(task.status);
		System.err.println(task.duration);
		System.err.println(task.speed);
	}

}