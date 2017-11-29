package org.tfelab.stock_qs.task;

import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.stock_qs.model.Stock;
import org.tfelab.txt.DateFormatUtil;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockInfoExtractTask extends Task {

	public static StockInfoExtractTask generateTask(String market, String code, AccountWrapper aw) {

		String url = "http://vip.stock.finance.sina.com.cn/corp/go.php/vCI_CorpInfo/stockid/" + code + ".phtml";

		try {
			StockInfoExtractTask t = new StockInfoExtractTask(market, code, url);
			t.setAccount(aw);
			return t;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

	private StockInfoExtractTask(String market, String code, String url) throws MalformedURLException, URISyntaxException {
		super(url);
		this.setParam("code", code);
		this.setParam("market", market);
	}

	public List<Task> postProc() {

		String src = getResponse().getText();

		Stock stock = new Stock(getParamString("market") + getParamString("code"));

		Map<String, String> patterns = new HashMap<>();
		patterns.put("market", "(?s)上市市场：.+?<td class=\"cc\">(?<T>.+?)</td>");
		patterns.put("name", "(?s)公司名称：.+?<td colspan=\"3\" class=\"ccl\">(?<T>.+?)</td>");
		patterns.put("short_name", "(?s)<h1 id=\"stockName\">(?<T>.+?)<span>");
		patterns.put("name_en", "(?s)公司英文名称：.+?<td colspan=\"3\" class=\"ccl\">(?<T>.+?)</td>");
		patterns.put("issue_price", "(?s)发行价格：</td>.+?<td class=\"cc\">(?<T>.+?)</td>");
		patterns.put("org_type", "(?s)机构类型：</td>.+?<td class=\"cc\">(?<T>.+?)</td>");
		patterns.put("org_form", "(?s)组织形式：</td>.+?<td class=\"cc\">(?<T>.*?)</td>");
		patterns.put("registered_capital", "(?s)注册资本：</td>.+?<td class=\"cc\">(?<T>.+?)元</td>");
		patterns.put("establishment_date", "(?s)成立日期：</td>.+?<td class=\"cc\">.+?>(?<T>.+?)</a></td>");
		patterns.put("listing_date", "(?s)上市日期：</td>.+?<td class=\"cc\">.+?>(?<T>.+?)</a></td>");

		for(String key : patterns.keySet()) {

			Pattern p = Pattern.compile(patterns.get(key));
			Matcher m = p.matcher(src);

			if(m.find()) {

				try {
					Field f = stock.getClass().getDeclaredField(key);

					if(f.getType().equals(Date.class)) {
						f.set(stock, DateFormatUtil.parseTime(m.group("T")));
					}
					else if (f.getType().equals(float.class)) {
						f.set(stock, DateFormatUtil.parseFloat(m.group("T")));
					}
					else if (f.getType().equals(double.class)) {
						f.set(stock, DateFormatUtil.parseDouble(m.group("T")));
					}
					else {
						f.set(stock, m.group("T"));
					}

				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

		}

		try {
			stock.insert();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
}