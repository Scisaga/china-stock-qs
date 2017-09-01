package org.tfelab.io.requester;

import com.typesafe.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;
import org.tfelab.common.config.Configs;
import org.tfelab.proxy.ProxyWrapper;
import org.tfelab.txt.ChineseChar;
import org.tfelab.util.StringUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 基本HTTP内容请求器
 * 
 * @author karajan
 *
 */
public class BasicRequester extends Requester {
	
	protected static BasicRequester instance;
	
	public static int CONNECT_TIMEOUT;
	public static int READ_TIMEOUT;
	
	private static final Logger logger = LogManager.getLogger(BasicRequester.class.getName());

	private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

	static {
		// read config
		Config ioConfig = Configs.dev.getConfig("io");
		CONNECT_TIMEOUT = ioConfig.getInt("connectTimeout");
		READ_TIMEOUT = ioConfig.getInt("readTimeout");

		System.setProperty("http.keepAlive", "false");
		System.setProperty("http.maxConnections", "100");
		System.setProperty("sun.net.http.errorstream.enableBuffering", "true");

		// 信任证书
		System.setProperty("javax.net.ssl.trustStore", "cacerts");
		// Cookie 接收策略
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

	}

	CookiesHolderManager cookiesHolderManager = null;
	
	/**
	 * 单例模式
	 * @return
	 */
	public static BasicRequester getInstance() {
		
		if (instance == null) {
			synchronized (BasicRequester.class) {
				if (instance == null) {
					instance = new BasicRequester();
				}
			}
		}

		return instance;
	}

	/**
	 *
	 */
	private BasicRequester() {

		cookiesHolderManager = new CookiesHolderManager();
	}
	
	/**
	 * 同步请求
	 * @param task
	 */
	public void fetch(Task task) {
		
		task.setStartTimeStamp();
		Wrapper wrapper = new Wrapper(task);
		wrapper.run();
		wrapper.close();
		/*wrapper = null;*/
	}
	
	/**
	 * 异步请求
	 * @param task
	 * @param timeout 可以手工设定延迟
	 */
	public void fetch(Task task, long timeout) {

		task.setStartTimeStamp();
		Wrapper wrapper = new Wrapper(task);
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future<?> future = executor.submit(wrapper);
		executor.shutdown();
		
		try {
			future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e){

			// Task 将抛出 java.io.IOException: Stream closed
			future.cancel(true);
		}

		wrapper.close();
		
		if (!executor.isTerminated()){
			executor.shutdownNow();
		}
	}

	/**
	 * 解压缩GZIP输入流
	 *
	 * @param input
	 * @return
	 */
	public static InputStream decompress_stream(InputStream input) {

		PushbackInputStream pb = new PushbackInputStream(input, 2); //we need a pushbackstream to look ahead

		byte[] signature = new byte[2];
		try {
			pb.read(signature);//read the signature
			pb.unread(signature); //push back the signature to the stream
		} catch (IOException e) {
			logger.warn(e.toString());
		}

		if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b)

			try {
				return new GZIPInputStream(pb);
			} catch (IOException e) {
				logger.warn(e.toString());
				return pb;
			}

		else
			return pb;
	}

	/**
	 * Parse out a charset from a content type headers.
	 *
	 * @param contentType e.g. "text/html; charset=EUC-JP"
	 * @return "EUC-JP", or null if not found. Charset is trimmed and
	 * uppercased.
	 */
	public static String getCharsetFromContentType(String contentType) {

		if (contentType == null)
			return null;

		Matcher m = charsetPattern.matcher(contentType);
		if (m.find()) {
			return m.group(1).trim().toUpperCase();
		}

		return null;
	}

	/**
	 * 文本内容自动解码方法
	 * @param src
	 * @param firstEncode
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String autoDecode(byte[] src, String firstEncode) throws UnsupportedEncodingException {
		
		String text;
		
		/**
		 * firstEncode != null
		 */
		if(firstEncode != null){
			
			logger.trace("charset detected = {}", firstEncode);
			try {
				text = new String(src, firstEncode);
			} catch (UnsupportedEncodingException err) {
				logger.trace("decoding using {}", "utf-8");
				text = new String(src, "utf-8");
			}
		
		} else {
			
			text = new String(src, "utf-8");
			Pattern pattern = Pattern.compile("meta.*?charset\\s*?=\\s*?([^\"' ]+)", Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(text);
			
			if (matcher.find()) {
				String encode = matcher.group(1);
				
				try {
					logger.trace("try decoding using {}", encode);
					text = new String(src, encode);
				} catch (Throwable ignored) {
					//src = new String(srcBin);
					logger.info("decoding error: {}", ignored.getMessage());
				}
			} else {
				
				UniversalDetector detector = new UniversalDetector(null);
				detector.handleData(src, 0, src.length);
				detector.dataEnd();
				String charset = detector.getDetectedCharset();
				
				if (charset != null) {
					logger.trace("charset detected = {}", charset);
					try {
						text = new String(src, charset);
					} catch (UnsupportedEncodingException err) {
						logger.trace("decoding using {}", "utf-8");
						text = new String(src, "utf-8");
					}
				} else {
					text = new String(src, "utf-8");
				}
			}
		}
		
		try {
			text = ChineseChar.unicode2utf8(text);
		} catch (Exception e){
			logger.error("Error convert unicode to utf8", e);
		} catch (Error e){
			logger.error("Error convert unicode to utf8", e);
		}
		
		return text;
	}
	
	/**
	 * HttpURLConnection工厂类
	 * 
	 * @author karajan
	 *
	 */
	public static class ConnectionBuilder {
		
		HttpURLConnection conn;
		
		/**
		 * 
		 * @param url
		 * @param pw
		 * @throws MalformedURLException
		 * @throws IOException
		 * @throws CertificateException 
		 * @throws KeyStoreException 
		 * @throws NoSuchAlgorithmException 
		 * @throws KeyManagementException 
		 */
		public ConnectionBuilder(String url, ProxyWrapper pw) throws MalformedURLException, IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
			
			if (pw != null) {

				Authenticator.setDefault(new ProxyAuthenticator(pw.getUsername(), pw.getPassword()));

				conn = (HttpURLConnection) new URL(url).openConnection(pw.toProxy());

				if (pw.needAuth()) {
					conn.setRequestProperty("Proxy-Switch-Ip","yes");
					String headerKey = "Proxy-Authorization";
					conn.addRequestProperty(headerKey, pw.getAuthenticationHeader());
				}

			} else {
				conn = (HttpURLConnection) new URL(url).openConnection();
			}
			
			if(url.matches("^https.+?$"))
				((HttpsURLConnection) conn).setSSLSocketFactory(CertAutoInstaller.getSSLFactory());
			
			conn.setConnectTimeout(CONNECT_TIMEOUT);

			conn.setReadTimeout(READ_TIMEOUT);
			
			conn.setDoOutput(true);
			
		}
		
		/**
		 * 定义Header
		 * @param header
		 */
		public void withHeader(Map<String, String> header) {

			if(header != null) {
				for(String key: header.keySet()) {
					conn.setRequestProperty(key, header.get(key));
				}
			}
		}
		
		/**
		 * 定义Post Data
		 * @param postData
		 * @throws IOException
		 */
		public void withPostData (String postData) throws IOException {
			
			//logger.info(postData);
			
			if (postData != null && !postData.isEmpty()) {
				conn.setDoInput(true);
				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print(postData);
				out.flush();
			}
		}
		
		/**
		 * 
		 * @return
		 */
		public HttpURLConnection build() {
			//conn.setAllowUserInteraction(false);
			return conn;
		}
	}

	/**
	 * HeaderBuilder
	 * 
	 * 构建请求所需的Header字段
	 */
	public static class HeaderBuilder {
		
		static String UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36";
		static String Accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
		static String AcceptLanguage = "zh-CN,zh;q=0.8";
		static String AcceptEncoding = "zip"; //, deflate, sdch
		static String AcceptCharset = "utf-8,gb2312;q=0.8,*;q=0.8";
		static String Connection = "Keep-Alive";
		
		/**
		 * Build headers from url, cookies and ref url
		 * @param url
		 * @param cookie
		 * @param ref
		 * @return
		 * @throws URISyntaxException
		 * @throws MalformedURLException
		 */
		public static Map<String, String> build(String url, String cookie, String ref)
				throws URISyntaxException, MalformedURLException
		{
			
			String host = StringUtil.getDomainName(url);
			
			Map<String, String> header = new TreeMap<String, String>();
			header.put("Host", host);
			header.put("User-Agent", UserAgent);
			header.put("Accept", Accept);
			header.put("Accept-Language", AcceptLanguage);
			header.put("Accept-Encoding", AcceptEncoding);
			header.put("Accept-Charset", AcceptCharset);
			header.put("Cache-Control", "no-cache");
			header.put("Connection", Connection);
			header.put("Upgrade-Insecure-Requests", "1");
			header.put("Pragma", "no-cache");
			if(cookie != null)
				header.put("Cookie", cookie);
			if(ref != null)
				header.put("Referer", ref);
			
			return header;
		}
	}
	
	/**
	 * Request Wraper Object
	 * 
	 * @author karajan
	 *
	 */
	public class Wrapper implements Runnable {
		
		Task task;
		
		Map<String, String> headers = null;
		HttpURLConnection conn = null;
		BufferedInputStream inStream = null;
		ByteArrayOutputStream bOutStream = null;
		
		boolean retry = false;
		int retry_count = 0;
		
		/**
		 * 
		 * @param task
		 */
		public Wrapper(Task task) {
			this.task = task;
		}
		
		/**
		 *
		 */
		public void run() {
			
			retry_count ++;
			
			if(retry_count > 2) return;
			
			logger.info(task.getUrl() + (task.getProxyWrapper() == null? "" : " via " + task.getProxyWrapper().getInfo()));
			
			try {

				String cookies = null;
				CookiesHolderManager.CookiesHolder cookiesHolder = null;
				String host = task.getProxyWrapper() == null ? "" : task.getProxyWrapper().getHost();
				if(task.getCookies() != null) {
					cookies = task.getCookies();
				} else {
					cookiesHolder = cookiesHolderManager.getCookiesHolder(host, task.getDomain());
					cookies = cookiesHolder == null? null : cookiesHolder.v;
				}


				if(task.getHeaders() != null) {
					headers = task.getHeaders();
				} else {
					headers = HeaderBuilder.build(task.getUrl(), task.getCookies(), task.getRef());
				}
				
				ConnectionBuilder connBuilder = new ConnectionBuilder(task.getUrl(), task.getProxyWrapper());
				connBuilder.withHeader(headers);
				connBuilder.withPostData(task.getPostData());
				conn = connBuilder.build();

				int code = 0;
				try {

					inStream = new BufferedInputStream(conn.getInputStream());
					code = conn.getResponseCode();
				} 
				catch (NoSuchElementException | SocketException | SocketTimeoutException e) {

					logger.error("Error Code: {}", code);
					throw e;
				} 
				catch (SSLException e){
					
					logger.warn("Encounter: {}", e.getMessage());
					
					try {
						
						CertAutoInstaller.installCert(task.getDomain(), StringUtil.getPort(task.getUrl()));
						// 重新获取
						connBuilder = new ConnectionBuilder(task.getUrl(), task.getProxyWrapper());
						connBuilder.withHeader(headers);
						connBuilder.withPostData(task.getPostData());
						conn = connBuilder.build();
						inStream = new BufferedInputStream(conn.getInputStream());
						
					} catch (Exception e1){
						task.setException(e1);
					}
				}
				catch (IOException e) {
					logger.error("Error Code: {}", code);
					task.setException(e);
					inStream = new BufferedInputStream(conn.getErrorStream());
				}

				task.getResponse().setHeader(conn.getHeaderFields());
				
				for (Map.Entry<String, List<String>> entry : task.getResponse().getHeader().entrySet()) {
					
					/**
					 * 解压缩
					 */
					if (entry.getKey() != null && entry.getKey().equals("Content-Encoding")) {
		
						if (entry.getValue().get(0).equals("gzip")) {
							inStream = new BufferedInputStream(decompress_stream(inStream));
						}
					}
					/**
					 * SET ENCODE
					 */
					if (entry.getKey() != null && entry.getKey().equals("Content-Type")) {
		
						for (String val : entry.getValue()) {
							if (val.matches(".*?charset=.+?")) {
								task.getResponse().setEncoding(
										val.replaceAll(".*?charset=", "").replaceAll(";", "").toUpperCase()
								);
							}
						}
					}
					/**
					 * Set-Cookie
					 */
					if (entry.getKey() != null && entry.getKey().equals("Set-Cookie")) {
						
						String newCookies = "";
						HashMap<String, String> cookie_map = new HashMap<String, String>();
						
						for(String val : entry.getValue()){
							String[] item = val.split("; *");
							for(String kv : item){
								String[] kv_ = kv.split("=");
								if(kv_.length>1 && ! kv_[0].matches("[Pp]ath|[Ee]xpires|[Dd]omain")) {
									cookie_map.put(kv_[0], kv_[1]);
								}
									
							}
						}
						
						for(String key : cookie_map.keySet()) {
							newCookies += key + "=" + cookie_map.get(key) + "; ";
						}
						
						newCookies = BasicRequester.mergeCookies(cookies, newCookies);
						task.getResponse().setCookies(newCookies);
						
						if(task.getCookies() == null) {
							if(cookiesHolder != null) {
								cookiesHolder.v = newCookies;
							} else {
								cookiesHolder = new CookiesHolderManager.CookiesHolder(newCookies);
							}
							cookiesHolderManager.addCookiesHolder(host, task.getDomain(), cookiesHolder);
						}	
					}
				}
				
				byte[] buf = new byte[1024];
				bOutStream = new ByteArrayOutputStream();
				
				// possible read timeout here
				int size;
				while ((size = inStream.read(buf)) > 0) {
					bOutStream.write(buf, 0, size);
				}
				
				task.getResponse().setSrc(bOutStream.toByteArray());
				
				if(task.getResponse().isText()) {
					task.getResponse().setText();
				}
				
				if(task.getResponse().getText() != null) {
					handleRefreshRequest(task);
				}
			}
			catch (Exception e){
				task.setException(e);
			}
			finally {
				
			}
			
			if(retry) {
				close();
				run();
			}
		}
		
		/**
		 * 处理内容跳转页面
		 * @param task
		 * @throws SocketTimeoutException
		 * @throws IOException
		 * @throws Exception
		 */
		private void handleRefreshRequest(Task task) throws SocketTimeoutException, IOException, Exception {
			
			Pattern p = Pattern.compile("(?is)<META HTTP-EQUIV=REFRESH CONTENT=['\"]\\d+;URL=(?<T>[^>]+?)['\"]>");
			Matcher m = p.matcher(task.getResponse().getText());
			
			if(m.find()){
				task.setUrl(m.group("T"));
				retry = true;
			}
		}
		
		/**
		 * 
		 */
		public void close() {
			
			if(task != null) {
				if (bOutStream != null) {
			        try {
			        	bOutStream.close();
			        } catch (IOException e) {
						task.setException(e);
			        }
			    }
			    if (inStream != null) {
			        try {
			        	inStream.close();
			        } catch (IOException e) {
						task.setException(e);
			        }
			    }
			    try {
				    if (conn != null) {
				    	conn.disconnect();
				    }
			    } catch (Exception e) {
					task.setException(e);
			    }
			    
			    task.setDuration();
			}
		}
	}

	/**
	 * 辅助方法 终端打印 Cookies
	 * @param cookies
	 */
	public static void printCookies(String cookies) {
		
		Map<String, String> map = new TreeMap<String, String>();
		
		if(cookies != null && cookies.length() > 0) {
			String[] cookie_items = cookies.split(";");
			for(String cookie_item : cookie_items) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {
					
					map.put(kv[0], kv[1]);
				}
			}
		}
		
		for(String k: map.keySet()){
			System.out.println(k + "=" + map.get(k) + "; "); 
		}

	}
	
	/**
	 * 合并Cookies
	 * @param cookies1
	 * @param cookies2
	 * @return
	 */
	public static String mergeCookies(String cookies1, String cookies2) {
		
		String cookies = "";
		Map<String, String> map = new HashMap<String, String>();
		
		if(cookies1 != null && cookies1.length() > 0) {
			String[] cookie_items1 = cookies1.split(";");
			for(String cookie_item : cookie_items1) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {
					map.put(kv[0], kv[1]);
				}
			}
		}
		
		if(cookies2 != null && cookies2.length() > 0) {
			String[] cookie_items2 = cookies2.split(";");
			for(String cookie_item : cookie_items2) {
				cookie_item = cookie_item.trim();
				String[] kv = cookie_item.split("=", 2);
				if(kv.length > 1) {
					map.put(kv[0], kv[1]);
				}
			}
		}
		
		for(String key : map.keySet()) {
			cookies += key + "=" + map.get(key) + "; ";
		}
		
		return cookies;
	}
}

class ProxyAuthenticator extends Authenticator {

    private String user, password;

    public ProxyAuthenticator(String user, String password) {
        this.user = user;
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password.toCharArray());
    }
}


