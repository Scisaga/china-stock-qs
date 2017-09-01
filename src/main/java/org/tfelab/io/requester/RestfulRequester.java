package org.tfelab.io.requester;

import com.google.gson.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.json.JSONable;
import org.tfelab.util.FormatUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 * == RestfulRequester.java ==
 * 基于URL参数的简单接口加密请求方法类
 * 
 * === 基本使用方法 ===
 * 1. 初始化uid和私钥
 *  RestfulRequester.getInstance().updateUidAndPrivateKey(String uid, String privateKey);
 * 
 * 2. 发起请求获得结果
 *  String resp = RestfulRequester.getInstance().request(String route, RequestType type, Map<String,?> query);
 * 或
 *  String resp = RestfulRequester.getInstance().request(String route, RequestType type, String query);
 * 
 * @author karajan@tfelab.org
 * 2016-11-30
 */
public class RestfulRequester {
	
	private static final Logger logger = LogManager.getLogger(RestfulRequester.class.getSimpleName());
	
	static int CONNECT_TIMEOUT = 10000;
	static int READ_TIMEOUT = 10000;
	
	private static String uid = "0";
	private static String private_key = "";
	
	private static RestfulRequester instance;
	
	private static final String UTF8_BOM = "\uFEFF";
	
	public static final Gson gson = new GsonBuilder()
			.disableHtmlEscaping()
			.registerTypeAdapter(Date.class, new DateSerializer()).setDateFormat(DateFormat.LONG)
			.registerTypeAdapter(Date.class, new DateDeserializer()).setDateFormat(DateFormat.LONG)
			.registerTypeAdapter(Exception.class, new ExceptionSerializer())
			.registerTypeAdapter(Exception.class, new ExceptionDeserializer())
			.create();

	public enum RequestType {
		GET, POST, PUT, DELETE
	}

	/**
	 * 单例模式
	 * @return
	 */
	public synchronized static RestfulRequester getInstance() {

		if (instance == null) {
			instance = new RestfulRequester();
		}
		return instance;
	}

	/**
	 * 设置超时
	 * @param connectTimeout
	 * @param readTimeout
	 */
	public static void setTimeout(int connectTimeout, int readTimeout){
		CONNECT_TIMEOUT = connectTimeout;
		READ_TIMEOUT = readTimeout;
	}

	/**
	 * 更新 uid 和 私钥
	 * @param uid
	 * @param privateKey
	 */
	public synchronized void updateUidAndPrivateKey(String uid, String privateKey) {
		RestfulRequester.uid = uid;
		RestfulRequester.private_key = privateKey;
	}

	/**
	 *
	 * @param route
	 * @param type
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public String request(String route, RequestType type, JSONable<?> obj)
		throws Exception
	{
		return request(route, type, obj.toJSON());
	}

	/**
	 * 获取返回结果
	 * @param route 具体请求url
	 * @param type 请求类型
	 * @param query 请求数据 Map<String, ?> 格式
	 * @return
	 * @throws Exception
	 */
	public String request(String route, RequestType type, Map<String,?> query)
		throws Exception
	{
		return request(route, type, gson.toJson(query));
	}

	/**
	 * 获取返回结果
	 * @param route 具体请求url
	 * @param type 请求类型
	 * @param query 请求数据 String 格式
	 * @return
	 * @throws Exception
	 */
	public String request(String route, RequestType type, String query)
		throws Exception
	{

		String paramStr = query;

		logger.trace("Query: {}", query);

		HttpURLConnection conn;

		try {

			conn = open(route, type, paramStr);
			conn.setDoOutput(true);
			conn.setRequestMethod(type.name());

			if(type != RequestType.GET) {

				conn.setDoInput(true);
				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.print("_q=" + URLEncoder.encode(paramStr, "utf-8"));
				out.flush();
			}

		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			logger.error("Error generate request", e);
			throw new Exception("Error generate request");
		}

		return read(conn);
	}

	/**
	 * Generate URL
	 * @param route
	 * @param type
	 * @param paramStr
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	private static String generateURL(String route, RequestType type, String paramStr) throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException
	{

		// 时间戳
		long _t = System.currentTimeMillis();

		String _h = encode(paramStr, _t, private_key);

		String url = route + "?";

		if(type == RequestType.GET){
			url += "_u=" + uid + "&_q=" + URLEncoder.encode(paramStr, "utf-8") + "&_h=" + _h + "&_t=" + _t;
		} else {
			url += "_u=" + uid +"&_h=" + _h + "&_t=" + _t;
		}

		return url;
	}

	/**
	 * Open http connection
	 * @param route
	 * @param type
	 * @param paramStr
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	private HttpURLConnection open(String route, RequestType type, String paramStr)
		throws MalformedURLException, IOException, InvalidKeyException, NoSuchAlgorithmException
	{

		String url = generateURL(route, type, paramStr);

		logger.trace("{} {}", type, url);

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);

		return conn;
	}

	/**
	 *
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	private String read(HttpURLConnection conn)
		throws IOException
	{

		BufferedInputStream inStream = new BufferedInputStream(conn.getInputStream());

		byte[] buf = new byte[1024];
		ByteArrayOutputStream bOutStream = new ByteArrayOutputStream();

		int size = 0;
		while ((size = inStream.read(buf)) > 0) {
			bOutStream.write(buf, 0, size);
		}

		byte[] srcBin = bOutStream.toByteArray();


		bOutStream.close();
		inStream.close();
		conn.disconnect();

		String result = new String(srcBin,"UTF-8");
		result = removeUTF8BOM(result);
		return result;
	}

	/**
	 *
	 * @param s
	 * @return
	 */
	private static String removeUTF8BOM(String s) {
		if (s.startsWith(UTF8_BOM)) {
			s = s.substring(1);
		}
		return s;
	}

	/**
	 * SHA256 encode
	 * @param param
	 * @param t
	 * @param private_key
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws UnsupportedEncodingException
	 */
	public static String encode(String param, long t, String private_key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException{

//		logger.trace(param);
//		logger.trace(t);
//		logger.trace(private_key);

		String raw = param + String.valueOf(t);
		SecretKey pKey = new SecretKeySpec(private_key.getBytes(), "HMACSHA256");
		Mac mac;
		mac = Mac.getInstance("HmacSHA256");
		mac.init(pKey);

		byte[] digest = mac.doFinal(raw.getBytes());

		return new String(Base64.encodeBase64(digest, false, true), "UTF-8");
	}

	static class DateDeserializer implements JsonDeserializer<Date> {

		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new Date(json.getAsJsonPrimitive().getAsLong());
		}
	}

	static class DateSerializer implements JsonSerializer<Date> {
		public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.getTime());
		}
	}

	static class ExceptionDeserializer implements JsonDeserializer<Exception> {
		public Exception deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new Exception(json.getAsJsonPrimitive().getAsString());
		}
	}

	static class ExceptionSerializer implements JsonSerializer<Exception> {
		public JsonElement serialize(Exception src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.getMessage());
		}
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		System.err.println(new String(Base64.encodeBase64(FormatUtil.hexStringToByteArray("24ec143e87b87ea10cf70c13c4fec6e6fe9c27994b534b29c558e4c2dc98fc5f"), false, true), "UTF-8"));
	}
}
