package org.tfelab.stock_qs.task;

import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.stock_qs.model.Stock;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarketScanTask extends Task {

	static List<String> codes = new ArrayList<>();

	public static MarketScanTask generateTask(String node, int page, AccountWrapper aw) {

		if(page > 10000) return null;

		String url = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=" + page + "&num=40&sort=symbol&asc=1&node=" + node + "&symbol=&_s_r_a=init";

		try {
			MarketScanTask t = new MarketScanTask(node, page, url);
			t.setAccount(aw);
			return t;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

	private MarketScanTask(String node, int page, String url) throws MalformedURLException, URISyntaxException {
		super(url);
		this.setParam("node", node);
		this.setParam("page", page);
	}

	public List<Task> postProc() {

		List<Task> tasks = new LinkedList<>();

		int stock_count = 0;

		try {

			String src = getResponse().getText();

			Pattern p = Pattern.compile("symbol:\"(?<m>.{2})(?<c>\\d{6})\"");
			Matcher m = p.matcher(src);

			while (m.find()) {


				if(Stock.getStockByCode(m.group("m") + m.group("c")) == null) {
					Task s_task = StockInfoExtractTask.generateTask(m.group("m"), m.group("c"), this.getAccountWrapper());
					tasks.add(s_task);
					//codes.add(m.group("m") + m.group("c"));
				}

				stock_count ++;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		if( stock_count > 0) {
			tasks.add(generateTask(getParamString("code"), getParamInt("page") + 1, this.getAccountWrapper()));
		}

		return tasks;
	}
}