package org.tfelab.stock_qs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RBlockingQueue;
import org.redisson.client.RedisTimeoutException;
import org.tfelab.common.Configs;
import org.tfelab.db.RedissonAdapter;
import org.tfelab.json.JSON;
import org.tfelab.io.requester.BasicRequester;
import org.tfelab.io.requester.Requester;
import org.tfelab.io.requester.chrome.ChromeDriverRequester;
import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.proxy.ProxyWrapper;
import org.tfelab.stock_qs.model.Proxy;
import org.tfelab.io.requester.proxy.IpDetector;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.txt.DateFormatUtil;
import org.tfelab.util.NetworkUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.Date;

public class Crawler {

	public static final Logger logger = LogManager.getLogger(Crawler.class.getName());
	public static String LOCAL_IP = IpDetector.getIp() + " :: " + NetworkUtil.getLocalIp();

	protected static Crawler instance;

	public static Crawler getInstance() {

		if (instance == null) {
			synchronized (ProxyManager.class) {
				if (instance == null) {
					instance = new Crawler();
				}
			}
		}

		return instance;
	}

	static int REQUEST_PER_SECOND_LIMIT = 20;

	// 单次请求TIMEOUT
	static int CONNECT_TIMEOUT = 120000;
	static int RETRY_LIMIT = Integer.MAX_VALUE; // 无限重试

	// 初始化配置参数
	static {

		try {
			REQUEST_PER_SECOND_LIMIT = Configs.getConfig(Requester.class).getInt("requestPerSecondLimit");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private Map<Class<? extends Task>, Distributor<? extends Task>> distributors = new HashMap<>();

	private Crawler() {

	}

	public boolean tasksDone(Class<? extends Task> clazz) throws InterruptedException {

		int count = 0;
		Distributor d = distributors.get(clazz);
		if(d == null) return true;

		for(int i=0; i<3; i++) {
			count += d.taskQueue == null? 0 : d.taskQueue.size();
			count += d.executor.getActiveCount();
			count += d.executor.getQueue().size();
			Thread.sleep(4000);
		}

		if(count == 0) return true;
		return false;
	}

	/**
	 *
	 * @param clazz
	 */
	void createDistributor(Class<? extends Task> clazz) {
		Distributor d = new Distributor(clazz.getSimpleName() + "-queue", clazz);
		d.setPriority(5);
		d.start();
		distributors.put(clazz, d);
	}

	/**
	 *
	 * @param t
	 */
	public void addTask(Task t) {

		if(t == null) return;

		if (distributors.get(t.getClass()) == null) {
			createDistributor(t.getClass());
		}

		if(t.isPrior()) {
			try {
				distributors.get(t.getClass()).distribute(t.toJSON());
			} catch (Exception e) {
				logger.error("Error add prior task. ", e);
			}
		} else {
			distributors.get(t.getClass()).taskQueue.offer(t.toJSON());
		}

	}

	/**
	 *
	 * @param ts
	 */
	public void addTask(List<Task> ts) {

		if(ts == null) return;

		for(Task t : ts) {
			addTask(t);
		}
	}

	/**
	 * 任务指派类
	 */
	class Distributor<T extends Task> extends Thread {

		RBlockingQueue<String> taskQueue;

		private volatile boolean done = false;

		ThreadPoolExecutor executor =  new ThreadPoolExecutor(
				2 * REQUEST_PER_SECOND_LIMIT,
				4 * REQUEST_PER_SECOND_LIMIT,
				0, TimeUnit.MICROSECONDS,
				new ArrayBlockingQueue<Runnable>(1000000));

		Class<T> clazz;

		/**
		 *
		 * @param taskQueueName
		 * @param clazz
		 */
		public Distributor (String taskQueueName, Class<T> clazz) {

			taskQueue = RedissonAdapter.redisson.getBlockingQueue(taskQueueName);
			taskQueue.clear();

			executor.setThreadFactory(new ThreadFactoryBuilder()
					.setNameFormat(taskQueueName + "Operator-Worker-%d").build());

			this.clazz = clazz;

			this.setName(this.getClass().getSimpleName() + "-" + taskQueueName);
		}

		/**
		 *
		 */
		public void run() {

			logger.info("Distributor {} started.", this.getName());

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}

			while(!done) {

				T t = null;
				String json = null;
				try {

					json = taskQueue.take();
					distribute(json);

				} catch (InterruptedException e) {
					logger.error(e);
				} catch (RedisTimeoutException e) {
					logger.error(e);
				} catch (Exception e) {
					logger.error("{}", json, e);
					System.exit(0);
				}
			}
		}

		public void distribute(String json) throws Exception {

			T t = JSON.fromJson(json, clazz);
			ProxyWrapper proxy = null;

			/**
			 * 根据AccountWrapper设定proxy
			 */
			if(t.getAccountWrapper() != null) {
				if(t.getAccountWrapper().getProxyId() != null) {
					proxy = Proxy.getProxyById(t.getAccountWrapper().getProxyId());
					if(proxy != null) {
						t.setProxyWrapper(proxy);
					} else {
						logger.error("No available proxy. Exit.");
						System.exit(0);
					}

				}
				else if(t.getAccountWrapper().getProxyGroup() != null) {
					proxy = Proxy.getValidProxy(t.getAccountWrapper().getProxyGroup());
					if(proxy != null) {
						t.setProxyWrapper(proxy);
					} else {
						logger.error("No available proxy. Exit.");
						System.exit(0);
					}
				}
			}

			/*// TODO Degenerated Case.
			if(t.getRetryCount() > 1) {
				t.setProxy(Proxy.getValidProxy("aliyun"));
			}*/

			Operator o = new Operator(t);
			if(proxy != null) {
				ProxyManager.getInstance().waits(proxy);
			}

			if(Thread.currentThread().getClass().equals(Distributor.class))
				waitForOperators();

			executor.submit(o);

			logger.info("Executor task queue active: {}, queue: {} ", executor.getActiveCount(), executor.getQueue().size());
		}

		/**
		 *
		 * @throws InterruptedException
		 */
		private void waitForOperators() throws InterruptedException {

			if(executor.getQueue().size() > 6 * REQUEST_PER_SECOND_LIMIT) {

			 	long sleepTime = 1 * Math.round(executor.getQueue().size() / ( REQUEST_PER_SECOND_LIMIT));

				logger.warn("Wait for operators {}s.", sleepTime);
				Thread.sleep(sleepTime * 1000);
			}
		}

		public void setDone() {
			this.done = true;
		}

		/**
		 *
		 */
		class Operator implements Runnable {

			T t;

			public Operator(T t) {
				this.t = t;
			}

			public void run() {

				t.setResponse();

				if(t.getRequester_class() != null
						&& t.getRequester_class().equals(ChromeDriverRequester.class.getSimpleName()))
				{
					ChromeDriverRequester.getInstance().fetch(t, CONNECT_TIMEOUT);
				} else {
					BasicRequester.getInstance().fetch(t, CONNECT_TIMEOUT);
				}

				StatManager.getInstance().count();

				/**
				 * 重试逻辑
				 */
				if (t.getException() != null) {

					logger.error("Fetch Error: {}.", t.getUrl(), t.getException());

					if(t.getRetryCount() < RETRY_LIMIT) {
						t.addRetryCount();
						t.addExceptions(t.getException().getMessage());
						addTask(t);
					} else {
						try {
							t.insert();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					return;

				} else {

					try {
						addTask(t.postProc());
					} catch (Exception e) {
						logger.error("Error in task post process. ", e);

						try {
							t.addExceptions(e.getMessage());
							t.insert();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				logger.info("{} duration: {}", t.getUrl(), t.getDuration());

			}
		}
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		StatManager.getInstance();

		if (args != null && args.length == 3 && args[0].toLowerCase().equals("transaction")) {

			String sd = args[1];
			String ed = args[2];

			Util.getAllTransactions(sd, ed);

		}
		else if (args != null && args.length > 0 && args[0].toLowerCase().equals("timequote")) {
			Util.getAllTimeQuotes("2017-01-03", DateFormatUtil.dfd.print(new Date().getTime()));
		}
		else if (args != null && args.length == 1 && args[0].toLowerCase().equals("refetch")) {

			Util.refetchTasks();
		}
		else {

		}
	}
}
