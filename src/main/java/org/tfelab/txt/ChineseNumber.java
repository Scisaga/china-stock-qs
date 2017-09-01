/**
 * 将汉字数字转换为数字
 */
package org.tfelab.txt;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chenshijiang
 * @date 2014年11月13日 下午8:49:49
 */
public class ChineseNumber {
	public ChineseNumber() {
	}

	private static Pattern CHN_NUM_PATTERN = Pattern.compile("[一二三四五六七八九][十百千]?");
	private static Map<Character, Integer> CHN_UNITS = new HashMap<Character, Integer>();
	private static Map<Character, Integer> CHN_NUMS = new HashMap<Character, Integer>();

	static {
		CHN_UNITS.put('十', 10);
		CHN_UNITS.put('百', 100);
		CHN_UNITS.put('千', 1000);
		CHN_UNITS.put('万', 10000);
		CHN_UNITS.put('亿', 10000000);
		CHN_NUMS.put('零', 0);
		CHN_NUMS.put('〇', 0);
		CHN_NUMS.put('一', 1);
		CHN_NUMS.put('二', 2);
		CHN_NUMS.put('三', 3);
		CHN_NUMS.put('四', 4);
		CHN_NUMS.put('五', 5);
		CHN_NUMS.put('六', 6);
		CHN_NUMS.put('七', 7);
		CHN_NUMS.put('八', 8);
		CHN_NUMS.put('九', 9);
	}

	/**
	 * 将小于一万的汉字数字，转换为BigInteger
	 *
	 * @param chnNum
	 * @return
	 */
	private static BigInteger getNumber(String chnNum) {
		BigInteger number = BigInteger.valueOf(0);
		Matcher m = CHN_NUM_PATTERN.matcher(chnNum);
		m.reset(chnNum);
		while (m.find()) {
			String subNumber = m.group();
			if (subNumber.length() == 1) {
				number = number
						.add(BigInteger.valueOf(CHN_NUMS.get(subNumber.charAt(0))));
			} else if (subNumber.length() == 2) {
				number = number
						.add(BigInteger.valueOf(CHN_NUMS.get(subNumber.charAt(0)))
								.multiply(
										BigInteger.valueOf(CHN_UNITS.get(subNumber
												.charAt(1)))));
			}
		}
		return number;
	}

	/**
	 * 将汉字转换为数字
	 *
	 * @param num
	 * @return
	 */
	public static int parseNumber(String chnNum) {
		chnNum = chnNum.replaceAll("(?<![一二三四五六七八九])十", "一十").replaceAll("零", "");
		Pattern pattern = Pattern.compile("[万亿]");
		Matcher m = pattern.matcher(chnNum);
		BigInteger result = BigInteger.valueOf(0);
		int index = 0;
		while (m.find()) {
			int end = m.end();
			int multiple = CHN_UNITS.get(m.group().charAt(0));
			String num = chnNum.substring(index, m.start());
			result = result.add(getNumber(num)).multiply(BigInteger.valueOf(multiple));
			index = end;
		}
		String num = chnNum.substring(index);
		result = result.add(getNumber(num));
		return result.intValue();
	}

	public static String covertStringWithChineseNumber(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (char c : input.toCharArray()) {
			if (CHN_NUMS.containsKey(c)) {
				sb.append(CHN_NUMS.get(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}