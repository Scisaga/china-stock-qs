package org.tfelab.stock_qs.cache;

import org.tfelab.stock_qs.model.Stock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Cache {

	public static Set<String> codes = new HashSet<String>();

	static {
		List<Stock> stocks = null;
		try {
			stocks = Stock.getAllStocks();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(Stock stock : stocks) {
			codes.add(stock.code);
		}
	}

}
