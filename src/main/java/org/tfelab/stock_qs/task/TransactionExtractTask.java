package org.tfelab.stock_qs.task;

import com.google.common.collect.ImmutableList;
import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.account.AccountWrapperImpl;
import org.tfelab.stock_qs.Crawler;
import org.tfelab.stock_qs.model.Stock;
import org.tfelab.stock_qs.model.TaskTrace;
import org.tfelab.stock_qs.model.Transaction;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.txt.DateFormatUtil;
import org.tfelab.txt.NumberFormatUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class TransactionExtractTask extends Task {

	/**
	 *
	 * @param code
	 * @param dateStr
	 * @param page
	 * @param accountWrapper
	 * @return
	 */
	public static TransactionExtractTask generateTask(String code, String dateStr, int page, AccountWrapper accountWrapper) {

		if(page > 10000) return null;

		if(Transaction.existRecords(code, dateStr)) {

			Crawler.logger.info("{} {} @{} already done.", code, TransactionExtractTask.class.getSimpleName(), dateStr);

			TaskTrace taskTrace;
			try {
				taskTrace = new TaskTrace(code, TransactionExtractTask.class.getSimpleName(), DateFormatUtil.parseTime(dateStr));
				taskTrace.insert();
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		String url = "http://market.finance.sina.com.cn/transHis.php?symbol=" + code + "&date=" + dateStr + "&page=" + page;
		try {
			TransactionExtractTask t =  new TransactionExtractTask(code, dateStr, page, url);
			t.setAccount(accountWrapper);
			return t;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 *
	 * @param code
	 * @param dateStr
	 * @param page
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	private TransactionExtractTask(String code, String dateStr, int page, String url) throws MalformedURLException, URISyntaxException {

		super(url);

		this.setParam("code", code);
		this.setParam("dateStr", dateStr);
		this.setParam("page", page);
	}

	/**
	 * 自定义后处理方法
	 * 解析并保存数据
	 * 同时生成翻页任务
	 */
	public List<Task> postProc() {

		String dateStr = getParamString("dateStr");
		String code = getParamString("code");
		int page = getParamInt("page");

		List<Transaction> ts = new LinkedList<>();

		try {

			String src = getResponse().getText();

			Pattern recordPattern = Pattern.compile("<tr ><th>(?<time>\\d+:\\d+:\\d+)</th><td>(?<price>\\d+\\.\\d+)</td><td>(?<change>(-?\\d+(\\.\\d+)?|--))</td><td>(?<volume>\\d+)</td><td>(?<turnover>\\d+(,\\d+)*(\\.\\d+)?)</td><th><h\\d>(?<type>.{2,3})</h\\d></th></tr>");
			Matcher recordMatcher = recordPattern.matcher(src);
			while (recordMatcher.find()) {

				Date time = DateFormatUtil.parseTime(dateStr + " " + recordMatcher.group("time"));
				float price = Float.parseFloat(recordMatcher.group("price"));

				String change_value_str = recordMatcher.group("change");
				if (change_value_str.equals("--")) change_value_str = "0";
				float change_value = Float.parseFloat(change_value_str);

				long volume = Long.parseLong(recordMatcher.group("volume")) * 100;
				double turnover = NumberFormatUtil.parseDouble(recordMatcher.group("turnover"));

				String type = recordMatcher.group("type");

				Transaction transaction = new Transaction(code, time, price, change_value, volume, turnover, type);

				ts.add(transaction);
			}

			if(ts.size() > 0) {

				try {
					Transaction.insertBatch(ts);
				} catch (Exception e) {
					e.printStackTrace();
				}

				Task t = generateTask(code, dateStr, ++page, this.getAccountWrapper());
				if(t != null) {
					t.setPrior();
					return ImmutableList.of(t);
				}


			} else {

				TaskTrace taskTrace = null;
				try {
					taskTrace = new TaskTrace(code, TransactionExtractTask.class.getSimpleName(), DateFormatUtil.parseTime(dateStr));
					taskTrace.insert();
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) throws Exception {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g);

		Crawler crawler = Crawler.getInstance();

		for(Stock st : Stock.getAllStocks()) {
			Task t = TransactionExtractTask.generateTask(st.code, "2017-01-01", 1, accountWrapper);
			crawler.addTask(t);
		}
	}
}
