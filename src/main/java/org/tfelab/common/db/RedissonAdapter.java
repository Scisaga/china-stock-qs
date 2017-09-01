package org.tfelab.common.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.tfelab.common.config.Configs;

/**
 * Created by Luke on 1/25/16. 
 * mailto:stormluke1130@gmail.com
 */
public class RedissonAdapter {

	public final static Logger logger = LogManager.getLogger(RedissonAdapter.class.getName());

	public static final RedissonClient redisson;

	static {
		try {
			Config config = new Config();
			logger.info("Connecting Redis...");

			config.useSingleServer()
				.setAddress(Configs.dev.getString("redis.url"))
				.setPassword(Configs.dev.getString("redis.password"))
				.setConnectionPoolSize(30)
				.setSubscriptionConnectionPoolSize(20)
				.setTimeout(10000)
				.setFailedAttempts(3)
				.setRetryAttempts(3)
				.setRetryInterval(1000);
			
			redisson = Redisson.create(config);
			logger.info("Connected to Redis");
		} catch (Throwable err) {
			logger.error("Redis init error", err);
			throw err;
		}
	}

}
