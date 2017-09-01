package org.tfelab.musical_sharing;

import com.j256.ormlite.dao.Dao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.db.OrmLiteDaoManager;
import org.tfelab.common.db.PooledDataSource;
import org.tfelab.common.db.Refacter;
import org.tfelab.io.requester.Task;
import org.tfelab.musical_sharing.model.AccessConfidential;
import org.tfelab.musical_sharing.model.User;

import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Helper {

	private static final Logger logger = LogManager.getLogger(Helper.class.getName());

	/**
	 * 谨慎使用
	 */
	public static void initDB() {

		logger.info("Init db tables...");

		try {

			Refacter.dropTables("org.tfelab.musical_sharing.model.User");
			Refacter.createTables("org.tfelab.musical_sharing.model.User");

			logger.info("Create db tables done.");

		} catch (Exception e) {
			logger.error("Error create tables.", e);
		}
	}

	public static void mergeUserIdQueue() {

		int i = 0;
		for(String uid: Crawler.userIdQueue) {

			Crawler.userIdQueueSet.add(uid);
			if(i++ % 1000 == 1) {
				System.err.println(i);
			}
		}
		Crawler.userIdQueue.clear();
		Crawler.userIdQueue.addAll(Crawler.userIdQueueSet);

	}

	public static void loadUserIds() throws Exception {

		List<String> uids = new LinkedList<>();

		Connection conn = PooledDataSource.getDataSource("musical_sharing").getConnection();

		String sql = "SELECT uid FROM users;";

		Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

		stmt.setFetchSize(Integer.MIN_VALUE);

		String uid = null;
		int i = 0;

		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			uid = rs.getString(1);
			uids.add(uid);

			i++;
			if(i % 10000 == 0) {
				System.err.println(i);
				//if(!Crawler.userIds.contains(uid) && !Crawler.userIdQueueSet.contains(uid)) {
//				Crawler.userIdQueue.addAll(uids);
//				Crawler.userIdQueueSet.addAll(uids);
				Crawler.userIds.addAll(uids);
				uids.clear();
				//}
			}
		}

		conn.close();

	}

	public static void main(String[] args) throws Exception {

		loadUserIds();
		// 重建特定表
		/*Refacter.dropTable(User.class);
		Refacter.createTable(User.class);*/
	}
}
