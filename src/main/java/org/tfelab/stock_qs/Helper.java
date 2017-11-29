package org.tfelab.stock_qs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.db.PooledDataSource;
import org.tfelab.db.Refacter;
import org.tfelab.stock_qs.model.*;

public class Helper {

	private static final Logger logger = LogManager.getLogger(Helper.class.getName());

	/**
	 * 谨慎使用
	 */
	public static void initDB() {

		logger.info("Init db tables...");

		try {

			Refacter.dropTables("org.tfelab.stock_qs.model");
			Refacter.createTables("org.tfelab.stock_qs.model");

			logger.info("Create db tables done.");

		} catch (Exception e) {
			logger.error("Error create tables.", e);
		}
	}


	public static void main(String[] args) throws Exception {
		//initDB();

		Refacter.createTable(Transaction.class);
	}
}
