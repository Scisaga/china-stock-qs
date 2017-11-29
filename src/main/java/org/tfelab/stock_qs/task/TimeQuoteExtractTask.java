package org.tfelab.stock_qs.task;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.tfelab.json.JSON;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.chrome.action.ChromeDriverAction;
import org.tfelab.io.requester.chrome.ChromeDriverRequester;
import org.tfelab.io.requester.Task;
import org.tfelab.stock_qs.Crawler;
import org.tfelab.stock_qs.model.TaskTrace;
import org.tfelab.stock_qs.model.TimeQuote;
import org.tfelab.txt.DateFormatUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeQuoteExtractTask extends Task {

	/**
	 *
	 * @param code
	 * @param dateStr
	 * @param accountWrapper
	 * @return
	 */
	public static TimeQuoteExtractTask generateTask(String code, String dateStr, AccountWrapper accountWrapper) {

		if(TimeQuote.existRecords(code, dateStr)) {

			Crawler.logger.info("{} {} @{} already done.", code, TimeQuoteExtractTask.class.getSimpleName(), dateStr);

			TaskTrace taskTrace;
			try {
				taskTrace = new TaskTrace(code, TimeQuoteExtractTask.class.getSimpleName(), DateFormatUtil.parseTime(dateStr));
				taskTrace.insert();
			} catch (Exception e) {

			}

			return null;
		}

		String url = "http://finance.sina.com.cn/h5charts/tchart.html?symbol=" + code + "&date=" + dateStr + "&rangeselector=true&indicator=tvol";
		try {

			TimeQuoteExtractTask t = new TimeQuoteExtractTask(code, dateStr, url);
			t.setRequester_class(ChromeDriverRequester.class.getSimpleName());
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
	 * @param url
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 */
	private TimeQuoteExtractTask(String code, String dateStr, String url) throws MalformedURLException, URISyntaxException {

		super(url);
		this.setParam("code", code);
		this.setParam("dateStr", dateStr);
		//this.addAction(new SetWindowSizeAction(380, 600));
		this.addAction(new ExtractAction(code, dateStr));
	}

	class ExtractAction extends ChromeDriverAction {

		public String code;
		public String dateStr;

		public ExtractAction() {}

		public ExtractAction(String code, String dateStr) {
			this.code = code;
			this.dateStr = dateStr;
		}

		@Override
		public boolean run(ChromeDriver driver) throws Exception {

			Thread.sleep(1000);

			String title_date = driver.getTitle().replaceAll("^.*?\\(", "").replaceAll("\\).*?$", "");
			if(!title_date.equals(dateStr)) {
				Crawler.logger.info("{} {} @{} done.", code, TimeQuoteExtractTask.class.getSimpleName(), dateStr);
				TaskTrace taskTrace = new TaskTrace(code, TimeQuoteExtractTask.class.getSimpleName(), DateFormatUtil.parseTime(dateStr));
				taskTrace.insert();
				return true;
			}

			// TODO chart 可能获取不到 如何处理?
			WebElement chart = driver.findElementById("chart");

			int width = chart.getSize().width - 110;
			int height = chart.getSize().height;
			double step = width / 240.0;

			Actions builder = new Actions(driver);

			Map<Long, TimeQuote> quotes = new HashMap<>();

			for(int i=55; i<width + 55; i++) {

				/*int x = 55 + (int) Math.round(step * i);*/

				try {

					builder.moveToElement(chart, i, height / 2).build().perform();

					String quoteSrc = driver.findElement(By.cssSelector("#chart > div > div:nth-child(6)")).getText();

					Pattern p = Pattern.compile(".+?(?<time>(19|20).+?)价格 (?<price>.+?)均价 .+?涨跌 (?<change>.+?)\\(.+?\\)成交 (?<volume>.+?)手");


					Matcher m = p.matcher(quoteSrc.replaceAll("\\r?\\n", ""));

					while (m.find()) {

					/*System.err.println(m.group());
					System.err.println(m.group("time").replaceAll("/", "-").replaceAll("-一", " "));*/

						Date time = DateFormatUtil.parseTime(
								m.group("time").replaceAll("/", "-").replaceAll("-[一二三四五六日七]", " ")
						);

						float price = DateFormatUtil.parseFloat(m.group("price"));

						long volume = DateFormatUtil.parseInt(m.group("volume")) * 100;

						float change_value = DateFormatUtil.parseFloat(m.group("change"));

						TimeQuote timeQuote = new TimeQuote(code, time, price, change_value, volume);

						quotes.put(time.getTime(), timeQuote);

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			try {
				TimeQuote.insertBatch(quotes.values());

				TaskTrace taskTrace = new TaskTrace(code, TimeQuoteExtractTask.class.getSimpleName(), DateFormatUtil.parseTime(dateStr));
				taskTrace.insert();
			} catch (Exception e) {

			}

			return true;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}
}
