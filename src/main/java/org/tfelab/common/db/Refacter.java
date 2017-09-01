package org.tfelab.common.db;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Refacter {

	public final static Logger logger = LogManager.getLogger(RedissonAdapter.class.getName());
	
	public static Class<? extends Annotation> annotationClass = DBName.class;

	public static List<Class<?>> classList = new ArrayList<Class<?>>();

	/**
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	public static void createTable(Class<? extends Object> clazz) throws Exception {
		Set<Class<? extends Object>> set = new HashSet<Class<? extends Object>>();
		set.add(clazz);
		createTables(set);
	}
	
	/**
	 * 
	 * @param clazz
	 * @throws Exception
	 */
	public static void dropTable(Class<? extends Object> clazz) throws Exception {
		Set<Class<? extends Object>> set = new HashSet<Class<? extends Object>>();
		set.add(clazz);
		dropTables(set);
	}
	
	/**
	 * 
	 * @param packageName
	 * @throws Exception
	 */
	public static void createTables(String packageName) throws Exception {
		createTables(getClasses(packageName));
	}
	
	/**
	 * 
	 * @param packageName
	 * @throws Exception
	 */
	public static void dropTables(String packageName) throws Exception {

		dropTables(getClasses(packageName));
	}

	/**
	 * get model class
	 * 
	 * @throws Exception
	 */
	private static Set<Class<? extends Object>> getClasses(String packageName) throws Exception {
		Reflections reflections = new Reflections(packageName);
		return reflections.getTypesAnnotatedWith(annotationClass);
	}
	
	/**
	 * 
	 * @param classes
	 */
	public static void createTables(Set<Class<? extends Object>> classes) {
		
		for (Class<?> clazz : classes) {
			
			logger.trace("Creating {}...", clazz.getName());
			
			ConnectionSource source;
			
			try {
				String dbName = clazz.getAnnotation(DBName.class).value();
				source = new DataSourceConnectionSource(
					PooledDataSource.getDataSource(dbName), 
					PooledDataSource.getDataSource(dbName).getJdbcUrl()
				);
				
				Dao<?, String> dao = OrmLiteDaoManager.getDao(clazz);
				if(!dao.isTableExists()){
					TableUtils.createTable(source, clazz);
					logger.trace("Created {}.", clazz.getName());
				} else {
					logger.info("Table {} already exists.", clazz.getName());
				}
				
			} catch (SQLException e) {
				logger.error("Error create table for {}.", clazz.getName(), e);
			} catch (Exception e) {
				logger.error("Error create table for {}.", clazz.getName(), e);
			}
		}
	}

	/**
	 * 
	 * @param classes
	 * @throws SQLException
	 */
	public static void dropTables(Set<Class<? extends Object>> classes) throws SQLException {
		for (Class<?> clazz : classes) {
			logger.trace("Dropping {}...", clazz.getName());
			ConnectionSource source;
			
			try {
				String dbName = clazz.getAnnotation(DBName.class).value();
				
				source = new DataSourceConnectionSource(
					PooledDataSource.getDataSource(dbName), 
					PooledDataSource.getDataSource(dbName).getJdbcUrl()
				);
				
				TableUtils.dropTable(source, clazz, true);
				logger.trace("Dropped {}.", clazz.getName());
				
			} catch (SQLException e) {
				logger.error("Error drop table for {}.", clazz.getName(), e);
			} catch (Exception e) {
				logger.error("Error drop table for {}.", clazz.getName(), e);
			}
		}
	}
}
