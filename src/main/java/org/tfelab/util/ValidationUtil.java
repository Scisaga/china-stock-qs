package org.tfelab.util;

import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationUtil {

	public static String item_seperator = "|||";
	public static String key_seperator = "=";
	

	public final static int OK = 0;
	public final static int AccountException = 1;
	public final static int TemplateException = 2;

	public static int validate(URLConnection conn, String src,
														 String account_validation, String header_validation,
														 String src_validation) {

		if (account_validation != null
				&& !ValidationUtil.verify(src, account_validation, false)) {
			return AccountException;
		}

		if (header_validation != null
				&& !ValidationUtil.verifyHeader(conn, header_validation)) {
			return TemplateException;
		}

		if (src_validation != null
				&& !ValidationUtil.verify(src, src_validation, true)) {
			return TemplateException;
		}

		return OK;
	}

	public static boolean verify(String src, String regs, boolean exist) {
		return verify(src, regs, item_seperator, exist);
	}

	/**
	 * 如果exist为true，当所有的正则表达式都通过时返回true 如果exist为false，当所有的正则表达式都没有通过时返回true
	 *
	 * @param src
	 * @param regs
	 * @param item_seperator
	 * @param exist
	 * @return
	 */
	public static boolean verify(String src, String regs,
															 String item_seperator, boolean exist) {
		String[] reg_list = regs.split(item_seperator);

		Pattern p;
		Matcher m;
		for (int i = 0; i < reg_list.length; i++) {
			p = Pattern.compile(reg_list[i]);
			m = p.matcher(src);
			if (m.find()) {
				if (!exist) {
					return false;
				}
			} else {
				if (exist) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean verifyHeader(URLConnection conn, String regs) {
		return verifyHeader(conn, regs, item_seperator, key_seperator);
	}

	/**
	 * 对regs中每一对key-reg，key对应的headerField能够通过正则表达式reg时，认为该条key-reg通过
	 * 所有key-reg都通过时，返回true
	 *
	 * @param conn
	 * @param regs
	 * @param reg_seperator
	 * @param key_seperator
	 * @return
	 */
	public static boolean verifyHeader(URLConnection conn, String regs,
																		 String reg_seperator, String key_seperator) {
		String[] item_list = regs.split(reg_seperator);

		Pattern p;
		Matcher m;
		for (int i = 0; i < item_list.length; i++) {
			int pos = item_list[i].indexOf(key_seperator);
			String key = item_list[i].substring(0, pos).trim();
			String value = item_list[i].substring(pos + key_seperator.length())
					.trim();

			p = Pattern.compile(value);
			m = p.matcher(conn.getHeaderField(key));

			if (!m.find()) {
				return false;
			}
		}

		return true;
	}
}
