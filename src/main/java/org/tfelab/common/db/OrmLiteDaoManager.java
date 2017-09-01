package org.tfelab.common.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * OrmLite 数据对象类连接管理器
 * @author karajan@tfelab.org
 * 2016年3月26日 下午4:17:05
 */
public class OrmLiteDaoManager {
	
	public final static Logger logger = LogManager.getLogger(OrmLiteDaoManager.class.getName());
	
	private static Map<Class<?>, Dao<?, String>> classDaoMap = new HashMap<Class<?>, Dao<?, String>>();
	
	/**
	 * 获取指定OrmLite 数据对象类的 Dao对象
	 * @param clazz
	 * @return
	 * @throws Exception 
	 * @throws SQLException
	 */
	public static synchronized <T> Dao<T, String> getDao(Class<T> clazz) throws Exception {
		
		if (classDaoMap.containsKey(clazz)) {
			return (Dao<T, String>) classDaoMap.get(clazz);
		}

		String dbName;

		try {
			dbName = clazz.getAnnotation(DBName.class).value();
		} catch (Exception e) {
			logger.error("Error get dbName annotation for {}.", clazz.getName(), e);
			throw new Exception("Error get dbName annotation for " + clazz.getName() + ".");
		}
		
		ConnectionSource source;
		
		try {
			
			source = new DataSourceConnectionSource(
				PooledDataSource.getDataSource(dbName), 
				PooledDataSource.getDataSource(dbName).getJdbcUrl()
			);
			
			Dao<T, String> dao = DaoManager.createDao(source, clazz);
			classDaoMap.put(clazz, dao);
			return dao;
			
		} catch (SQLException e) {
			logger.error("Error get DAO for {}.", dbName, e);
			throw new Exception("Error get DAO for " + dbName + ".");
		}
	}

	/**
	 * 直接执行SQL语句，需要指定数据库
	 * @param dbName
	 * @param sql
	 * @return
	 * @throws Exception 
	 * @throws SQLException
	 */
	public static Boolean execute(String dbName, String sql) throws Exception {
		
		try {
			
			ConnectionSource source = new DataSourceConnectionSource(
				PooledDataSource.getDataSource(dbName), 
				PooledDataSource.getDataSource(dbName).getJdbcUrl()
			);
			
			DatabaseConnection conn = source.getReadWriteConnection();
			return conn.executeStatement(sql,
					DatabaseConnection.DEFAULT_RESULT_FLAGS) == DatabaseConnection.DEFAULT_RESULT_FLAGS;
			
		} catch (SQLException e) {
			logger.error("Error execute {} for {}.", sql, dbName);
		}
		
		return false;
	}

	/**
	 * 
	 * @param clazz
	 * @return
	 * @throws Exception 
	 * @throws SQLException
	 */
	public static <T> Dao<T, String> getDaoSync(Class<T> clazz) throws Exception {
		return getDao(clazz);
	}
}