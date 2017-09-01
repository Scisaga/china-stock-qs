package org.tfelab.io.requester;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.common.config.Configs;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基本HTTP内容请求器
 * 
 * @author karajan
 *
 */
public class ChromeDriverRequester extends Requester {

	protected static ChromeDriverRequester instance;

	private static final Logger logger = LogManager.getLogger(ChromeDriverRequester.class.getName());

	private static int agentCount = 4;

	static {
		try {
			agentCount = Configs.dev.getConfig("io").getInt("chromeDriverAgentNum");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private BlockingQueue<ChromeDriverAgent> agentQueue = new LinkedBlockingQueue<ChromeDriverAgent>();

	private ConcurrentHashMap<String, ArrayList<String>> cookieStore = new ConcurrentHashMap<String, ArrayList<String>>();

	/**
	 * 单例模式
	 * 
	 * @return
	 */
	public static ChromeDriverRequester getInstance() {

		if (instance == null) {
			synchronized (ChromeDriverRequester.class) {
				if (instance == null) {
					instance = new ChromeDriverRequester();
				}
			}
		}

		return instance;
	}

	private ChromeDriverRequester() {

		for (int i = 0; i < agentCount; i++) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ChromeDriverAgent agent = new ChromeDriverAgent();
			this.agentQueue.add(agent);
		}
	}

	public void fetch(Task task) {

		ChromeDriverAgent agent = null;

		try {

			agent = this.agentQueue.take();
			agent.fetch(task);

		} catch (InterruptedException e) {

			logger.error("Can't get agent from queue, {}", e.toString());
			task.setException(new AgentNotAvailableException());

		} finally {

			if (agent != null) {
				agentQueue.add(agent);
			}
		}
	}

	public void fetch(Task task, long timeout) {

		ChromeDriverAgent agent = null;

		try {

			/**
			 * 此处有可能拿不到 agent
			 */
			agent = this.agentQueue.take();
			agent.fetch(task, timeout);

		} catch (InterruptedException e) {

			logger.error("Can't get agent from queue, {}", e.toString());
			task.setException(new AgentNotAvailableException());

		} finally {

			if (agent != null) {
				agentQueue.add(agent);
			}
		}
	}

	/**
	 * TODO 未完善退出逻辑
	 */
	public void close() {
		for (ChromeDriverAgent agent : this.agentQueue) {
			agent.close();
		}
		instance = null;
	}

	public static void close_() {
		if (instance != null) {
			instance.close();
		}
	}
}