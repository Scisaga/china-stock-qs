package org.tfelab.stock_qs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import static spark.Spark.port;

import java.util.Set;

import static spark.Spark.*;

public class ServiceWrapper {
	
	public static final Logger logger = LogManager.getLogger(ServiceWrapper.class.getName());
	
	private static ServiceWrapper instance;
	
	public static ServiceWrapper getInstance() {
		
		synchronized (ServiceWrapper.class) {
			if (instance == null) {
				instance = new ServiceWrapper();
			}
		}
		
		return instance;
	}
	
	public int port = 50000;
	
	public volatile boolean batchRematching = false;
	
	/**
	 * 
	 */
	public ServiceWrapper() {

		//Client_Port = Configs.dev.getInt("Client_Port");
		staticFiles.externalLocation("www");
		port(port);
		
		/**
		 * Using Reflection load routes
		 */
		Reflections reflections = new Reflections("org.tfelab.stock_qs.route", new SubTypesScanner(false));
		Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(Object.class);
		
		for(Class<? extends Object> clazz : allClasses) {
			if(clazz.getSimpleName().matches(".+?Route")) {
				logger.info("Add {}.", clazz.getSimpleName());
				try {
					clazz.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					logger.error(e);
					System.exit(1);
				}
			}
		}
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ServiceWrapper.getInstance();
	}

}
