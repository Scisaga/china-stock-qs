package org.tfelab.util;

import com.google.common.collect.ImmutableMap;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatUtil {

	public static DateTimeFormatter dfd = DateTimeFormat.forPattern("yyyy-MM-dd");
	public static DateTimeFormatter dff = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
	public static DateTimeFormatter dfm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
	public static DateTimeFormatter dft = DateTimeFormat.forPattern("HH:mm:ss");
	public static DateTimeFormatter dft1 = DateTimeFormat.forPattern("HH:mm");
	public static DateTimeFormatter dfn = DateTimeFormat.forPattern("yyyyMMdd");
	public static DateTimeFormatter dfn1 = DateTimeFormat.forPattern("dd-MM-yyyy");
	
	public static DateTimeFormatter dfd_en_1 = DateTimeFormat.forPattern("MMM dd, yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_2 = DateTimeFormat.forPattern("MMM dd, yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_11 = DateTimeFormat.forPattern("dd MMM, yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_21 = DateTimeFormat.forPattern("dd MMM, yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_12 = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_22 = DateTimeFormat.forPattern("dd MMM yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_3 = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_4 = DateTimeFormat.forPattern("MMM dd yy").withLocale(Locale.US);
	
	public static DateTimeFormatter dfd_en_5 = DateTimeFormat.forPattern("dd-MMM-yy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_51 = DateTimeFormat.forPattern("dd-MMM-yyyy").withLocale(Locale.US);
	
	public static DateTimeFormatter dfd_en_6 = DateTimeFormat.forPattern("MM/yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_61 = DateTimeFormat.forPattern("MMM yyyy").withLocale(Locale.US);
	public static DateTimeFormatter dfd_en_62 = DateTimeFormat.forPattern("yyyy MMM").withLocale(Locale.US);

	/**
	 * 将UUID转换为bin(16)
	 *
	 * @param uuid
	 * @return
	 */
	public static byte[] getIdAsByte(UUID uuid) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	/**
	 * 将bin(16)转换为UUID NOTE: 转换不是互逆的
	 *
	 * @param uuid
	 * @return
	 */
	public static UUID toUUID(byte[] bytes) {

		if (bytes.length != 16) {
			throw new IllegalArgumentException();
		}
		int i = 0;
		long msl = 0;
		for (; i < 8; i++) {
			msl = msl << 8 | bytes[i] & 0xFF;
		}
		long lsl = 0;
		for (; i < 16; i++) {
			lsl = lsl << 8 | bytes[i] & 0xFF;
		}
		return new UUID(msl, lsl);
	}

	/**
	 * 将byte[]转化为十进制字符串
	 *
	 * @param a
	 * @return
	 */
	public static String byteArrayToHex(byte[] a) {

		if (a == null) return "";

		StringBuilder sb = new StringBuilder(a.length * 2);

		for (byte b : a) {
			sb.append(String.format("%02x", b & 0xff));
		}

		return sb.toString();
	}

	/**
	 * 将十进制字符串转化为Byte[]
	 *
	 * @param s
	 * @return
	 */
	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
					.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	/**
	 * 一般性日期字符串解析方法
	 *
	 * @param in
	 * @return
	 * @throws ParseException
	 */
	@SuppressWarnings("deprecation")
	public static Date parseTime(String in) throws ParseException {
		
		if (in == null) {
			return new Date();
		}
		in = in.trim();
		
		String prefix = null;
		Pattern p = Pattern.compile("今天|昨天|前天|\\d+(天|分钟|小时)前");
		Matcher m = p.matcher(in);
		if(m.find()){
			prefix = m.group();
			in = in.replaceAll(prefix, "");
		}
		
		in = in.trim();
		Date date = new Date();
		
		String yyyyMMdd = Calendar.getInstance().get(Calendar.YEAR) + "-" + (Calendar.getInstance().get(Calendar.MONTH)+1) + "-" + Calendar.getInstance().get(Calendar.DATE);
		
		in = in.replaceAll("日", "")
				.replaceAll("年|月", "-")
				.replaceAll("/", "-")
				.replaceAll("\\.", "-")
				.replaceAll("T", " ").replaceAll("Z", "");

		// 以秒为单位
		if (in.matches("\\d{9,10}")) {
			return new Date(Long.parseLong(in + "000"));
		}
		// 以毫秒为单位
		else if (in.matches("\\d{12,13}")) {
			return new Date(Long.parseLong(in));
		}
		else if (in.matches("\\d{1,2}-\\d{1,2}-\\d+")) {
			return dfn1.parseDateTime(in).toDate();
		}
		// 默认格式
		else if (in.matches("[A-Za-z]{3,4} \\d{1,2}, \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} (AM|PM)")) {
			return new Date(in);
		}
		// yyyy-MM-dd HH:mm:ss
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2}")) {
			return dff.parseDateTime(in).toDate();
		}
		// yyyy-MM-dd HH:mm
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{1,2}")) {
			return dfm.parseDateTime(in).toDate();
		}
		// yyyy-MM-dd
		else if (in.matches("\\d{2,4}-\\d{1,2}-\\d{1,2}")) {
			return dfd.parseDateTime(in).toDate();
		}
		// MM-dd
		else if (in.matches("\\d{1,2}-\\d{1,2}")) {
			return dfd.parseDateTime(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + '-' + in).toDate();
		}
		// MM-dd HH:mm
		else if (in.matches("\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}")) {
			return dfm.parseDateTime(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + '-' + in).toDate();
		}
		// HH:mm:ss
		else if (in.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
			
			return new Date(dff.parseDateTime(yyyyMMdd + " " + in).toDate().getTime() + getShiftValue(prefix));
		}
		// HH:mm
		else if (in.matches("\\d{1,2}:\\d{2}")) {
			return new Date(dfm.parseDateTime(yyyyMMdd + " " + in).toDate().getTime() + getShiftValue(prefix));
		}
		// yyyyMMdd
		else if (in.matches("\\d{8}")) {
			return dfn.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd, yyyy -- Mar 3, 2016
		else if(in.matches("\\w+ +\\d{1,2} *, +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_1.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd, yy -- Mar 3, 16
		else if(in.matches("\\w+ +\\d{1,2} *, +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_2.parseDateTime(in).toDate();
		}
		// 英文日期格式dd MMM, yyyy -- Mar 3, 2016
		else if(in.matches("\\d{1,2} +\\w+ *, +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_11.parseDateTime(in).toDate();
		}
		// 英文日期格式 dd MMM, yy -- Mar 3, 16
		else if(in.matches("\\d{1,2} +\\w+ *, +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_21.parseDateTime(in).toDate();
		}
		// 英文日期格式dd MMM, yyyy -- Mar 3, 2016
		else if(in.matches("\\d{1,2} +\\w+ +\\d{4}")){
			in = in.replaceAll(" +", " ");
			return dfd_en_12.parseDateTime(in).toDate();
		}
		// 英文日期格式 dd MMM, yy -- Mar 3, 16
		else if(in.matches("\\d{1,2} +\\w+ +\\d{2}")){
			in = in.replaceAll(" +", " ");
			return dfd_en_22.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd yyyy -- Mar 3, 2016
		else if(in.matches("\\w+ +\\d{1,2} +\\d{4}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_3.parseDateTime(in).toDate();
		}
		// 英文日期格式 MMM dd yy -- Mar 3, 16
		else if(in.matches("\\w+ +\\d{1,2} +\\d{2}")){
			in = in.replaceAll(" +,", ",").replaceAll(" +", " ");
			return dfd_en_4.parseDateTime(in).toDate();
		}
		// 英文日期格式01-Nov-15
		else if(in.matches("\\d{1,2}-\\w+-\\d{2}")){
			return dfd_en_5.parseDateTime(in).toDate();
		}
		// 英文日期格式01-Nov-2015
		else if(in.matches("\\d{1,2}-\\w+-\\d{4}")){
			return dfd_en_51.parseDateTime(in).toDate();
		}
		// 英文日期格式10/2016
		else if(in.matches("\\d{1,2}/-\\d{4}")){
			return dfd_en_6.parseDateTime(in).toDate();
		}
		// 英文日期格式June 2016
		else if(in.matches("\\w+ \\d{4}")){
			return dfd_en_61.parseDateTime(in).toDate();
		}
		// 英文日期格式2016 June
		else if(in.matches("\\d{4} \\w+")){
			return dfd_en_62.parseDateTime(in).toDate();
		}
		// 英文日期格式21 Oct
		else if(in.matches("\\d{1,2} \\w+")){
			return dfd_en_12.parseDateTime(in + " " + Calendar.getInstance().get(Calendar.YEAR)) .toDate();
		}
		// 英文日期格式Oct 21
		else if(in.matches("\\w+ \\d{1,2}")){
			return dfd_en_3.parseDateTime(in + " " + Calendar.getInstance().get(Calendar.YEAR)).toDate();
		}
		else if (prefix != null){
			return new Date(new Date().getTime() + getShiftValue(prefix));
		}
		// 不能解析的情况
		else {
			return date;
		}
	}
	
	/**
	 * 
	 * @param prefix
	 * @return
	 */
	private static long getShiftValue(String prefix){
		long v = 0;
		if(prefix == null){
			
		} else if(prefix.equals("今天")){
			
		} else if (prefix.equals("昨天")){
			v = - 24 * 60 * 60 * 1000;
		} else if (prefix.equals("前天")){
			v = - 2 * 24 * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+天前")){
			int n = Integer.parseInt(prefix.replaceAll("天前", ""));
			v = - n * 24 * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+小时前")){
			int n = Integer.parseInt(prefix.replaceAll("小时前", ""));
			v = - n * 60 * 60 * 1000;
		} else if (prefix.matches("\\d+分钟前")){
			int n = Integer.parseInt(prefix.replaceAll("分钟前", ""));
			v = - n * 60 * 1000;
		}
		
		return v;
	}

	/**
	 * 去掉多余的空格和HTML标记
	 *
	 * @param in
	 * @return
	 */
	public static String purgeHTML(String in) {
		if (in == null) {
			return null;
		}

		return in.replaceAll("^[ |	|　]+", "").replaceAll("[ |	|　]+$", "")
				.replaceAll(">[ |	|　]+", ">").replaceAll("[ |	|　]+<", "<")
				.replaceAll("&nbsp;", "").replaceAll("style=\".+?\"", "");

	}
	
	/**
	 * 
	 * @param in
	 * @return
	 */
	public static double parseDouble(String in) {

		double v = 0;
		
		Map<String, Double> map = new HashMap<>();

		map.put("百", 100D);
		map.put("千", 1000D);
		map.put("万", 10000D);
		map.put("百万", 100 * 10000D);
		map.put("亿", 10000 * 10000D);
		map.put("K", 1000D);
		map.put("k", 1000D);
		map.put("M", 1000000D);
		map.put("m", 1000000D);
		
		List<Double> multis = new ArrayList<Double>();
		Pattern p = Pattern.compile("百|千|万|百万|亿|k|K|m|M");
		Matcher m = p.matcher(in);
		while(m.find()){
			multis.add(map.get(m.group()));
		}
		
		in = in.trim();
		boolean negative = false;
		if(in.length() > 1 && in.subSequence(0, 1).equals("-")) {
			negative = true;
		}
		
		in = in.replaceAll("百|千|万|百万|亿|k|K|m|M", "").replaceAll("\\+|-", "").trim();
		
		if(in.matches("(\\d+\\.)?\\d+")){
			v = Double.parseDouble(in);
			for(Double ms : multis){
				v *= ms;
			}
		}
		
		return negative ? -v : v;
	}
	
	/**
	 * 
	 * @param in
	 * @return
	 */
	public static float parseFloat(String in){
		
		return (float) parseDouble(in);
	}
	
	/**
	 * 
	 * @param in
	 * @return
	 */
	public static int parseInt(String in){
		
		return (int) parseDouble(in);
	}

	/**
	 * 去除HTML标记
	 *
	 * @param in
	 * @return
	 */
	public static String removeHTML(String in) {
		return in.replaceAll("<[^>]+?>", "");
	}

	/**
	 * 去除空格符
	 *
	 * @param in
	 * @return
	 */
	public static String removeBlank(String in) {
		return in.replaceAll("\\s*", "");
	}
	
	
	public static void main(String[] args) {
		
		DateTimeFormatter dfd_en_1 = DateTimeFormat.forPattern("MMM dd, yyyy").withLocale(Locale.US);
		System.err.println(dfd_en_1.parseDateTime("August 3, 2016").toDate());
		
		System.err.println(parseFloat("-12.23"));
	}
}
