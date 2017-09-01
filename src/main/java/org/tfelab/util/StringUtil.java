/**
 *
 */
package org.tfelab.util;

import com.google.common.net.InternetDomainName;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @author chenshijiang
 * @date 2014年10月28日 下午9:25:00
 */
public class StringUtil {
	
	/**
	 * 对长出的字符串进行截断
	 */
	public static String strCrop(String in, int length) {
		if (in != null && in.length() > length) {
			return in.substring(0, length);
		} else
			return in;
	}
	
	/**
	 * 根据spliter第一次出现的位置
	 * 将输入字符串一分为二
	 * @param src
	 * @param spliter
	 * @return
	 */
	public static String[] splitFirstChar(String src, String spliter) {

		String[] res = new String[2];
		int eindex = src.indexOf(spliter);
		if (eindex < 0) {
			return null;
		}
		int slen = spliter.length();

		res[0] = src.substring(0, eindex);
		res[1] = src.substring(eindex + slen);
		return res;

	}
	
	/**
	 * 清理Ctrl-Char和反斜杠
	 *
	 * @param in
	 * @return
	 */
	public static String removeCtrlChars(String in) {
		String out = in;

		// 处理反斜杠
		out = out.replaceAll("\\\\", "\\\\\\\\");
		out = out.replaceAll("\\\\\\\"", "\\\\\\\\\"");
		out = out.replaceAll(" | ", "");

		out = out.replace("\n|\b|\f|\t|\r", "");

		// ascii 0-31 ctrl-char + ascii 127 del
		out = out.replaceAll("[\u0000-\u001f\u007f]", "");

		return out;
	}

	/**
	 * 生成16位UUID
	 *
	 * @return
	 */
	public static byte[] uuid(String src) {

		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");

			m.reset();
			m.update(src.getBytes());
			byte[] digest = m.digest();
			return digest;
		} catch (NoSuchAlgorithmException e) {

			UUID uid = UUID.randomUUID();
			return FormatUtil.getIdAsByte(uid);
		}
	}

	/**
	 * 转换成UTF-8
	 * 
	 * @param str
	 * @return
	 */
	public static String toUTF8(String str) {
		String result = str;
		try {
			result = new String(str.getBytes(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * MD5 encode
	 * @param inStr
	 * @return
	 */
	public static String MD5(String inStr) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}
		char[] charArray = inStr.toCharArray();
		byte[] byteArray = new byte[charArray.length];

		for (int i = 0; i < charArray.length; i++)
			byteArray[i] = (byte) charArray[i];

		byte[] md5Bytes = md5.digest(byteArray);

		StringBuffer hexValue = new StringBuffer();

		for (int i = 0; i < md5Bytes.length; i++) {
			int val = ((int) md5Bytes[i]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}

		return hexValue.toString();
	}
	
	public static String getDomainName(String url) throws URISyntaxException, MalformedURLException {
		URL uri = new URL(url);
	    String domain = uri.getHost();
	    return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
	
	public static String getRootDomainName(String domain) {
		return InternetDomainName.from(domain).topPrivateDomain().toString();
	}
	
	public static int getPort(String url) throws URISyntaxException, MalformedURLException {
		URL uri = new URL(url);
		
		int port = 80;
		
		if(uri.getProtocol().equals("https")){
			port = 443;
		}
		
		if(uri.getPort() > 0) {
			port = uri.getPort();
		}
		return port;
	}
	
	public static String getProtocol(String url) throws URISyntaxException, MalformedURLException {
		URL uri = new URL(url);
		return uri.getProtocol();
	}

	public static void main(String[] args) throws MalformedURLException, URISyntaxException {
	    System.err.println(getProtocol("https://pic.36krcnd.com/avatar/201609/08060207/6b5gr77ktodsf70j.jpg!heading"));
	    System.out.println(InternetDomainName.from("abc.ds2.taobao.com.cn").topPrivateDomain().toString());
	}
}
