package org.tfelab.stock_qs;

import com.j256.ormlite.dao.Dao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.db.OrmLiteDaoManager;
import org.tfelab.db.PooledDataSource;
import org.tfelab.io.requester.BasicRequester;
import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.account.AccountWrapperImpl;
import org.tfelab.stock_qs.model.*;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.stock_qs.task.MarketScanTask;
import org.tfelab.stock_qs.task.TimeQuoteExtractTask;
import org.tfelab.stock_qs.task.TransactionExtractTask;
import org.tfelab.stock_qs.util.TimeQuoteCalc;
import org.tfelab.txt.DateFormatUtil;
import org.tfelab.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

	public static final Logger logger = LogManager.getLogger(Util.class.getName());

	public static void getAllStockInfo() {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g);

		Task t = MarketScanTask.generateTask("sh_a", 1, accountWrapper);
		Task t_ = MarketScanTask.generateTask("sz_a", 1, accountWrapper);

		Crawler crawler = Crawler.getInstance();
		crawler.addTask(t);
		crawler.addTask(t_);

	}

	/**
	 *
	 * @param sd
	 * @param ed
	 * @throws Exception
	 */
	public static void getAllTransactions(String sd, String ed) throws Exception {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g);

		Crawler crawler = Crawler.getInstance();

		List<Stock> stockList = Stock.getAllStocks();

		for(Date date = DateFormatUtil.parseTime(sd);
			date.before(new Date(DateFormatUtil.parseTime(ed).getTime() + 86400 * 1000));
			date = new Date(date.getTime() + 86400 * 1000)) {

			final String dateStr = DateFormatUtil.dfd.print(date.getTime());
			for(Stock stock : stockList) {

				if(TaskTrace.getTaskTrace(stock.code, TransactionExtractTask.class, date) == null) {
					Task t = TransactionExtractTask.generateTask(stock.code, dateStr, 1, accountWrapper);
					crawler.addTask(t);
				} else {
					logger.info("{} {} @{} done.", stock.code, TransactionExtractTask.class.getSimpleName(), dateStr);
				}

			}

			/**
			 *
			 */
			while(!Crawler.getInstance().tasksDone(TransactionExtractTask.class)){
				Thread.sleep(8000);
			}
			logger.info("All stocks {} @{} done, cheers.", TransactionExtractTask.class.getSimpleName(), dateStr);
			Transaction.hashset.clear();
		}
	}

	/**
	 *
	 * @param sd
	 * @param ed
	 * @throws Exception
	 */
	public static void getAllTimeQuotes(String sd, String ed) throws Exception {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.aliyun_g);

		Crawler crawler = Crawler.getInstance();

		List<Stock> stockList = Stock.getAllStocks();

		for(Date date = DateFormatUtil.parseTime(sd);

			date.before(DateFormatUtil.parseTime(ed));
			date = new Date(date.getTime() + 86400 * 1000)) {

			final String dateStr = DateFormatUtil.dfd.print(date.getTime());

			for(Stock stock : stockList) {

				if(TaskTrace.getTaskTrace(stock.code, TimeQuoteExtractTask.class, date) == null) {
					Task t = TimeQuoteExtractTask.generateTask(stock.code, dateStr, accountWrapper);
					crawler.addTask(t);
				} else {
					logger.info("{} {} @{} done.", stock.code, TimeQuoteExtractTask.class.getSimpleName(), dateStr);
				}
			}
		}
	}

	/**
	 * 重试失败的采集任务
	 * @throws Exception
	 */
	public static void refetchTasks() throws Exception {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g);

		Crawler crawler = Crawler.getInstance();

		List<String> urls = new ArrayList<>();

		File file = new File("task.txt");
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));

			for(String tempString = null; (tempString = reader.readLine()) != null; urls.add(tempString)) {
				;
			}

			reader.close();
		} catch (IOException var13) {
			var13.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException var12) {
					var12.printStackTrace();
				}
			}

		}

		int i =0 ;
		for(String url : urls) {
			Pattern p = Pattern.compile("http://market\\.finance\\.sina\\.com\\.cn/transHis\\.php\\?symbol=(?<code>.+?)&date=(?<date>.+?)&page=(?<page>.+?)");
			Matcher m = p.matcher(url);
			if(m.find()) {
				String code = m.group("code");
				String dateStr = m.group("date");
				int page = Integer.valueOf(m.group("page"));

				Task t = TransactionExtractTask.generateTask(code, dateStr, page, accountWrapper);
				crawler.addTask(t);
			}

			i++;
			if(i % 100 == 0) {
				System.err.print(i + "..");
			}
		}



//		Dao<Task, String> dao = OrmLiteDaoManager.getDao(Task.class);
//		List<Task> tasks = dao.queryForAll();
//
//		for(Task task : tasks) {
//			String url = task.getUrl(); //http://market.finance.sina.com.cn/transHis.php?symbol=sh601128&date=2016-03-19&page=1
//			Pattern p = Pattern.compile("http://market\\.finance\\.sina\\.com\\.cn/transHis\\.php\\?symbol=(?<code>.+?)&date=(?<date>.+?)&page=(?<page>.+?)");
//			Matcher m = p.matcher(url);
//			if(m.find()) {
//				String code = m.group("code");
//				String dateStr = m.group("date");
//				int page = Integer.valueOf(m.group("page"));
//				Task t = TransactionExtractTask.generateTask(code, dateStr, page, accountWrapper);
//				crawler.addTask(t);
//				dao.delete(task);
//			}
//
//		}
	}

	/**
	 * 根据逐笔记录计算分时数据
	 * @param sd
	 * @param ed
	 * @throws Exception
	 */
	public static void calcTimeQuotes(String sd, String ed) throws Exception {
		List<Stock> stockList = Stock.getAllStocks();

		for(Date date = DateFormatUtil.parseTime(sd);
			date.before(new Date(DateFormatUtil.parseTime(ed).getTime() + 86400 * 1000));
			date = new Date(date.getTime() + 86400 * 1000)) {

			final String dateStr = DateFormatUtil.dfd.print(date.getTime());

			for(Stock stock : stockList) {

				if(Transaction.existRecordsFull(stock.code, dateStr)) {
					TimeQuote.insertBatch(TimeQuoteCalc.calc(stock.code, date));
				}
			}
		}
	}

	public static void checkTasks() throws Exception {

		String src = "";

		Connection conn = PooledDataSource.getDataSource("china_stock_qs").getConnection();
		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(Integer.MIN_VALUE);
		ResultSet rs = stmt.executeQuery("SELECT url FROM tasks");

		int i =0 ;
		while (rs.next()) {
			String url = rs.getString("url");

			Pattern p = Pattern.compile("http://market\\.finance\\.sina\\.com\\.cn/transHis\\.php\\?symbol=(?<code>.+?)&date=(?<date>.+?)&page=(?<page>.+?)");
			Matcher m = p.matcher(url);
			if(m.find()) {
				String code = m.group("code");
				String dateStr = m.group("date");
				int page = Integer.valueOf(m.group("page"));

				boolean fetched = Transaction.existRecords(code, dateStr);
				boolean fetched_ = Transaction.existRecords(code, dateStr);

				if (fetched != fetched_) {
					logger.info("{}\tPage:{}\tf1:{}\tf2:{}", url, page, fetched, fetched_);
				}

				if(page == 1 && !fetched || page > 1 && fetched) {
					src += url  + "\n";
				}
			}

			i++;
			if(i % 100 == 0) {
				System.err.print(i + "..");
			}
		}

		FileUtil.writeBytesToFile(src.getBytes(), "task.txt");
	}

	public static void checkMarketOpenDate() throws Exception {

		String dateSrc = "";
		Date sd = DateFormatUtil.parseTime("2010-01-01 00:00:00");
		Date ed = new Date();

		for(Date date = sd;
			date.before(ed);
			date = new Date(date.getTime() + 86400 * 1000)) {

			final String dateStr = DateFormatUtil.dfd.print(date.getTime());

			logger.info("==> {}", dateStr);

			String url = "http://market.finance.sina.com.cn/transHis.php?symbol=sh600000&date=" + dateStr;

			Task task = new Task(url);
			task.setProxyWrapper(Proxy.getProxyById("9"));
			BasicRequester.getInstance().fetch(task);

			if(task.getException() == null) {
				String src = task.getResponse().getText();
				if(src.contains("输入的代码有误或没有交易数据")) {

				} else {
					dateSrc += dateStr + "\n";
				}
			} else {
				logger.error(task.getException().getMessage());
			}

		}

		FileUtil.writeBytesToFile(dateSrc.getBytes(), "date.txt");

	}

	public static void testBanRules() throws Exception {

		String url = "https://www.baidu.com/s?wd=ip";

		for(int i = 0; i<1000; i++) {

			Thread.sleep(200);

			new Thread() {
				public void run() {
					Task task;
					try {
						task = new Task(url);
						task.setProxyWrapper(Proxy.getProxyById("12"));

						BasicRequester.getInstance().fetch(task);
						if(task.getException() != null) {
							logger.error(task.getException());
							//throw task.getException();
						} else {
							Pattern p = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
							Matcher m = p.matcher(task.getResponse().getText());
							while(m.find()) {
								if(!m.group().equals("8.8.8.8") && !m.group().contains("192.168"))
									System.err.println(m.group());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}


				}
			}.start();
			//System.err.println(i);


		}
	}


	public static void main(String[] args) throws Exception {
		testBanRules();
	}

}
