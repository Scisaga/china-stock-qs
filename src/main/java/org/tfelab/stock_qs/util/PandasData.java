package org.tfelab.stock_qs.util;

import com.google.common.collect.ImmutableList;
import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;
import org.tfelab.stock_qs.model.TimeQuote;
import org.tfelab.stock_qs.model.Transaction;
import org.tfelab.txt.DateFormatUtil;

import java.util.ArrayList;
import java.util.List;

public class PandasData {

	/**
	 *
	 */
	public static class Transactions implements JSONable {

		List columns = ImmutableList.of("code", "price", "change_value", "volume", "turnover", "type");
		List index = new ArrayList();
		List data = new ArrayList();

		public Transactions(List<Transaction> ts) {
			for(Transaction t: ts) {
				index.add(DateFormatUtil.dff.print(t.time.getTime()));
				data.add(t);
			}
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	public static class TimeQuotes implements JSONable {

		List columns = ImmutableList.of("code", "price", "change_value", "volume");
		List index = new ArrayList();
		List data = new ArrayList();

		public TimeQuotes(List<TimeQuote> ts) {
			for(TimeQuote t: ts) {
				index.add(DateFormatUtil.dff.print(t.time.getTime()));
				data.add(t);
			}
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}
}
