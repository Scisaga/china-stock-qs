package org.tfelab.io.requester;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tfelab.common.json.JSON;
import org.tfelab.musical_sharing.Crawler;
import org.tfelab.musical_sharing.model.AccessConfidential;
import org.tfelab.musical_sharing.model.User;
import org.tfelab.proxy.ProxyWrapper;
import org.tfelab.txt.ChineseChar;
import org.tfelab.util.FormatUtil;
import org.tfelab.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.tfelab.io.requester.BasicRequester.autoDecode;

/**
 * HTTP 请求任务
 * @author karajan
 *
 */
public class Task {
	
	private String url;
	private Map<String, String> headers;
	private String postData;
	private String cookies;
	private String ref;

	private String domain;

	// 代理服务器信息
	private ProxyWrapper pw;

	// 执行动作列表
	private List<ChromeDriverAction> actions = new ArrayList<>();

	// 返回对象
	private transient Response response;

	// 控制参数
	private boolean preProc = false;
	private boolean shootScrren = false;
	private boolean resetAgent = false;

	// 记录参数
	private long startTimeStamp;
	private long duration = 0;

	private int retryCount = 0;

	// 异常
	private Exception e;

	public Task() {
		this.response = new Response();
	}
	
	/**
	 * 简单 GET 请求
	 * @param url url地址
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url) throws MalformedURLException, URISyntaxException {
		this.url = url;
		
		domain = StringUtil.getDomainName(url);
		
		this.response = new Response();
	}
	
	/**
	 * 简单 POST 请求
	 * @param url url 地址
	 * @param postData post data 字符串格式
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String postData) throws MalformedURLException, URISyntaxException {
		
		this.url = url;
		this.postData = postData;
		domain = StringUtil.getDomainName(url);
		
		this.response = new Response();
	}
	
	/**
	 * 需要 Cookie 的 POST 请求
	 * @param url
	 * @param postData
	 * @param cookies
	 * @param ref
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, String postData, String cookies, String ref) throws MalformedURLException, URISyntaxException {
		
		this.url = url;
		this.postData = postData;
		this.cookies = cookies;
		this.ref = ref;
		domain = StringUtil.getDomainName(url);
		
		this.response = new Response();
	}

	/**
	 * 完整参数请求
	 * @param url
	 * @param headers
	 * @param postData
	 * @param cookies
	 * @param ref
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	public Task(String url, Map<String, String> headers, String postData, String cookies, String ref) throws MalformedURLException, URISyntaxException {

		this.url = url;
		this.headers = headers;
		this.postData = postData;
		this.cookies = cookies;
		this.ref = ref;
		domain = StringUtil.getDomainName(url);

		this.response = new Response();
	}
	
	/**
	 * 
	 * @param pw
	 */
	public void setProxy(ProxyWrapper pw) {
		this.pw = pw;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getPostData() {
		return postData;
	}

	public String getCookies() {
		return cookies;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getDomain() {
		return domain;
	}

	public ProxyWrapper getProxyWrapper() {
		return pw;
	}

	public void setProxyWrapper(ProxyWrapper pw) {
		this.pw = pw;
	}

	public List<ChromeDriverAction> getActions() {
		return actions;
	}

	public void setActions(List<ChromeDriverAction> actions) {
		this.actions = actions;
	}

	public void setResponse() {
		response = new Response();
	}

	public Response getResponse() {
		return response;
	}

	public boolean isPreProc() {
		return preProc;
	}

	public void setPreProc(boolean preProc) {
		this.preProc = preProc;
	}

	public boolean isShootScrren() {
		return shootScrren;
	}

	public void setShootScrren(boolean shootScrren) {
		this.shootScrren = shootScrren;
	}

	public boolean isResetAgent() {
		return resetAgent;
	}

	public void setResetAgent(boolean resetAgent) {
		this.resetAgent = resetAgent;
	}

	public long getStartTimeStamp() {
		return startTimeStamp;
	}

	public void setStartTimeStamp() {
		this.startTimeStamp = System.currentTimeMillis();
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration() {
		this.duration = System.currentTimeMillis() - this.startTimeStamp;
	}

	public Exception getException() {
		return e;
	}

	public void setException(Exception e) {
		this.e = e;
	}

	/*public static class Cookie {
		String hash;
		Map<String, String> v = new HashMap<String, String>();

		public Cookie (Map<String, String> cookies_map) {
			for(String key : cookies_map.keySet()) {
				v.put(key, cookies_map.get(key));
			}
		}

		public Cookie (String cookies_string) {

		}

	}*/

	public void postProc() {

	}

	public String toJSON() {
		return JSON.toJson(this);
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void addRetryCount() {
		this.retryCount ++;
	}

	/**
	 * 返回对象
	 * @author karajan
	 */
	public class Response {
		
		private Map<String, List<String>> header;
		private byte[] src;
		private String encoding;
		private String cookies;

		private boolean actionDone;

		private String text;

		private transient Document doc = null;

		private byte[] screenshot = null;

		private Exception e = null;

		public Map<String, List<String>> getHeader() {
			return header;
		}

		public void setHeader(Map<String, List<String>> header) {
			this.header = header;
		}

		public byte[] getSrc() {
			return src;
		}

		public void setSrc(byte[] src) {
			this.src = src;
		}

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public String getCookies() {
			return cookies;
		}

		public void setCookies(String cookies) {
			this.cookies = cookies;
		}

		public boolean isActionDone() {
			return actionDone;
		}

		public void setActionDone(boolean actionDone) {
			this.actionDone = actionDone;
		}

		public String getText() {
			return text;
		}

		public Document getDoc() {
			return doc;
		}

		public byte[] getScreenshot() {
			return screenshot;
		}

		public void setScreenshot(byte[] screenshot) {
			this.screenshot = screenshot;
		}

		public Exception getException() {
			return e;
		}

		public void setException(Exception e) {
			this.e = e;
		}

		/**
		 * 根据返回文本构建 DOM 对象
		 */
		void buildDom () {

			if(this.text != null && doc == null){

				doc = Jsoup.parse(text);
			}
		}

		/**
		 * 根据 CssQuery 查找对象
		 * @param cssQuery
		 * @return
		 */
		Elements findElements(String cssQuery) {

			if(doc == null) return null;

			return doc.select(cssQuery);
		}

		/**
		 * 根据 CssQuery 查找对象
		 * @param cssQuery
		 * @return
		 */
		Element findElement(String cssQuery) {

			if(doc == null) return null;

			return doc.select(cssQuery).first();
		}

		/**
		 * 判断 Response 是否为文本
		 * @return
		 */
		public boolean isText(){
			if(header == null) return true;
			if(header.get("Content-Type") != null){
				for(String item: header.get("Content-Type")){
					if((item.contains("application") && !item.contains("json") && !item.contains("xml") && !item.contains("x-javascript"))
						|| item.contains("video")
						|| item.contains("audio")
						|| item.contains("image")
					){
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * 文本内容预处理
		 * @param input 原始文本
		 * @throws UnsupportedEncodingException
		 */
		public void setText(String input) throws UnsupportedEncodingException {

			this.text = input;

			if(isPreProc()) {

				try {
					text = StringEscapeUtils.unescapeHtml4(text);
					text = ChineseChar.unicode2utf8(text);
				} catch (Exception e) {
					e.printStackTrace();
				}

				/* src = ChineseChar.unescape(src); */
				text = ChineseChar.toDBC(text);
				/* text = ChineseChar.toSimp(text); */
				text = text.replaceAll("　+|	+| +| +", " ");
				text = text.replaceAll(">\\s+", ">");
				text = text.replaceAll("\\s+<", "<");
				text = text.replaceAll("(\r?\n)+", "\n");
				/* src = src.replaceAll("<!\\[CDATA\\[|\\]\\]>", ""); */

				text = url + "\n" + postData + "\n" + text;
			}
		}

		/**
		 * 文本内容预处理
		 * @throws UnsupportedEncodingException
		 */
		public void setText() throws UnsupportedEncodingException {
			String input = autoDecode(src, encoding);
			setText(input);
		}
	}

	class Task_ extends Task {

		public String uid;

		public Task_(String url, Map<String, String> headers, String postData, String cookies, String ref, String uid) throws MalformedURLException, URISyntaxException {
			super(url, headers, postData, cookies, ref);
			this.uid = uid;
		}

		public void postProc() {

			try {

				String src = getResponse().getText().replaceAll("\r?\n", " ");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws MalformedURLException, URISyntaxException {
		Task t = new Task("https://www.baidu.com");
		System.err.println(t.toJSON());
		Task t_ = JSON.fromJson(t.toJSON(), Task_.class);
		System.err.println(t_.toJSON());
		BasicRequester.getInstance().fetch(t_);

		System.err.println(t_.getResponse());

	}
}
