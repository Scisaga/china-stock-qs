package org.tfelab.common.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.config.Configs;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author karajan
 * @date 2015年1月20日 下午6:19:34
 *
 */
public class PooledDataSource {
	
	public final static Logger logger = LogManager.getLogger(PooledDataSource.class.getName());
	
	public static Map<String, ComboPooledDataSource> CPDSList = new HashMap<String, ComboPooledDataSource>();

	/**
	 * 获取数据库链接
	 * @param dbName
	 * @return
	 * @throws Exception 
	 */
	public static synchronized ComboPooledDataSource getDataSource(String dbName) throws Exception{
		
		if(CPDSList.containsKey(dbName)){
			
			ComboPooledDataSource ds = CPDSList.get(dbName);
			logger.trace("{} --> max:{}, busy:{}", dbName, ds.getMaxPoolSize(), ds.getNumBusyConnections());
			
			return ds;
			
		} else {
			
			ComboPooledDataSource ds = addDataSource(dbName);
			logger.trace("{} --> max:{}, busy:{}", dbName, ds.getMaxPoolSize(), ds.getNumBusyConnections());
			
			return ds;
		}
	}
	
	/**
	 * 打开一个数据库连接
	 * @param dbName
	 * @return
	 * @throws Exception
	 */
	public static synchronized ComboPooledDataSource addDataSource(String dbName) throws Exception{
		
		ComboPooledDataSource cpds = null;
		cpds = new ComboPooledDataSource();
		
		try {
			Config config = Configs.dev.getConfig("database").getConfig(dbName);
			
			cpds.setDriverClass("com.mysql.jdbc.Driver");
			
			cpds.setJdbcUrl(config.getString("url"));
			cpds.setUser(config.getString("username"));
			cpds.setPassword(config.getString("password"));                                  
			
			cpds.setInitialPoolSize(config.getInt("initialPoolSize"));   
			cpds.setMinPoolSize(config.getInt("minPoolSize"));                                     
			cpds.setAcquireIncrement(config.getInt("acquireIncrement"));
			cpds.setMaxPoolSize(config.getInt("maxPoolSize"));
			cpds.setMaxStatements(config.getInt("maxStatements"));
			cpds.setMaxStatementsPerConnection(config.getInt("maxStatements"));
			
			cpds.setMaxConnectionAge(3600);
			cpds.setNumHelperThreads(5);
			cpds.setMaxIdleTimeExcessConnections(120);
			cpds.setMaxIdleTime(120);
			cpds.setAcquireRetryAttempts(0);
			cpds.setAcquireRetryDelay(1000);
			cpds.setIdleConnectionTestPeriod(120);
			
		} catch (ConfigException e) {
			logger.error("No {} confg infomation in config file.", dbName, e);
			throw new Exception("Open pooled db connection failed.");
		} catch (PropertyVetoException e) {
			logger.error("Class not found.", e);
			throw new Exception("Open pooled db connection failed.");
		}
		
		CPDSList.put(dbName, cpds);
		
		return cpds;
	}
	
	/**
	 * 关闭所有数据库连接池
	 */
	public static synchronized void close(){
		for(String key : CPDSList.keySet()){
			CPDSList.get(key).close();
		}
	}
}
