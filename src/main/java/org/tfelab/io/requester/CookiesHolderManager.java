package org.tfelab.io.requester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by karajan on 2017/6/3.
 */
public class CookiesHolderManager {

	private ConcurrentHashMap<String, Map<String, List<CookiesHolder>>> cookieStore = new ConcurrentHashMap<>();

	/**
	 *
	 * @param host
	 * @param domain
	 * @param cookieHolder
	 */
	public void addCookiesHolder(String host, String domain, CookiesHolder cookieHolder) {

		Map<String, List<CookiesHolder>> cookiesOneDomain = cookieStore.get(domain);

		if(cookiesOneDomain == null) {

			cookiesOneDomain = new HashMap<String, List<CookiesHolder>>();
			cookieStore.put(domain, cookiesOneDomain);
		}

		List<CookiesHolder> cookiesOneHost = cookiesOneDomain.get(host);
		if(cookiesOneHost == null) {
			cookiesOneHost = new ArrayList<CookiesHolder>();
			cookiesOneDomain.put(host, cookiesOneHost);
		}

		cookiesOneHost.add(cookieHolder);
	}

	public CookiesHolder getCookiesHolder(String host, String domain) {

		if(cookieStore.get(domain) == null || cookieStore.get(domain).get(host) == null
				|| cookieStore.get(domain).get(host).size() < 20) {
			return null;
		}
		else {

			CookiesHolder cookiesHolder = cookieStore.get(domain).get(host).remove(0);

			cookiesHolder.count ++;

			if(cookiesHolder.count > 40) {
				return null;
			} else {
				return cookiesHolder;
			}
		}
	}

	static class CookiesHolder {
		int count = 0;
		String v;

		CookiesHolder(String cookies) {
			this.v = v;
		}
	}
}