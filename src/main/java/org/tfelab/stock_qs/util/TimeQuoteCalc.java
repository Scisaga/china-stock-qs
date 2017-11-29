package org.tfelab.stock_qs.util;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.db.OrmLiteDaoManager;
import org.tfelab.stock_qs.Helper;
import org.tfelab.stock_qs.model.TimeQuote;
import org.tfelab.stock_qs.model.Transaction;
import org.tfelab.txt.DateFormatUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public class TimeQuoteCalc {

	private static final Logger logger = LogManager.getLogger(TimeQuoteCalc.class.getName());


	public static List<TimeQuote> calc(String code, Date date) throws Exception {

		// 获取昨日收盘价
		Dao<Transaction, String> dao = OrmLiteDaoManager.getDao(Transaction.class);
		Transaction lastTransaction = dao.queryBuilder().limit(1).orderBy("time", false).where()
				.eq("code", code)
				.and()
				.lt("time", date)
				.queryForFirst();

		float lastClosePrice = lastTransaction.price;

		logger.info("Stock {} last day close price: {}.", code, lastClosePrice);

		//
		String dateStr = DateFormatUtil.dfd.print(date.getTime());

		Date sd = DateFormatUtil.parseTime(dateStr + " 09:30:00");
		Date ed = DateFormatUtil.parseTime(dateStr + " 15:00:00");

		TreeMap<Date, List<Transaction>> transactionMap = new TreeMap<>();

		for(Date d = sd; d.before(ed); d = new Date(d.getTime() + 60 * 1000)){
			transactionMap.put(d, new ArrayList<Transaction>());
		}
		transactionMap.put(ed, new ArrayList<Transaction>());

		//
		List<Transaction> transactions = dao.queryBuilder().where()
				.eq("code", code)
				.and()
				.lt("time", new Date(date.getTime() + 24 * 60 * 60 * 1000))
				.and()
				.ge("time", date)
				.query();

		logger.info("Total {} transactions {} @{}", transactions.size(), code, dateStr);

		for(Transaction transaction : transactions) {

			for(Date dateKey : transactionMap.keySet()) {
				if(transaction.time.equals(dateKey) || transaction.time.after(dateKey) && transaction.time.before(new Date(dateKey.getTime() + 60 * 1000))) {
					transactionMap.get(dateKey).add(transaction);
					break;
				}
			}
		}

		List<TimeQuote> timeQuotes = new ArrayList<>();

		float price = 0;
		for(Date dateKey : transactionMap.keySet()) {

			if(price == 0)
				price = lastClosePrice;
			long volumn = 0;
			Date lastTransactionTime = null;

			for(Transaction transaction : transactionMap.get(dateKey)) {

				if(lastTransactionTime == null) {
					price = transaction.price;
					lastTransactionTime = transaction.time;
				} else if(transaction.time.after(lastTransactionTime)){
					price = transaction.price;
					lastTransactionTime = transaction.time;
				}

				volumn += transaction.volume;
			}

			float changeValue =
				new BigDecimal(price - lastClosePrice).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

			TimeQuote timeQuote = new TimeQuote(code, dateKey, price, changeValue, volumn);
			timeQuotes.add(timeQuote);

		}

		return timeQuotes;

	}

	public static void main(String[] args) {

		try {
			calc("sz300016", DateFormatUtil.parseTime("2017-01-09 00:00:00"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
