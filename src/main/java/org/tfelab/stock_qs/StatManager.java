package org.tfelab.stock_qs;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.stock_qs.model.CrawlerStat;

public class StatManager {
	
	private static StatManager instance;
	public final static Logger logger = LogManager.getLogger(StatManager.class.getName());
	
	public synchronized static StatManager getInstance() {

		if (instance == null) {
			instance = new StatManager();
		}
		return instance;
	}

	// 统计时间段
	public static long interval = 1000 * 60;
	// 周期任务Timer
	Timer timer = new Timer();

	volatile int counter = 0;
	
	public StatManager() {
		Updater updater = new Updater();
		timer.scheduleAtFixedRate(updater, 0, interval);
	}
	
	public void stop(){
		timer.cancel();
	}

	public void count() {
		counter ++;
	}

	/**
	 * 用于定期更新元素列表列表
	 * @author karajan
	 *
	 */
	class Updater extends TimerTask {

		/**
		 * 生成记录入库，清空已有统计量
		 */
		public void run() {

			CrawlerStat stat = new CrawlerStat(counter);

			try {
				stat.insert();
			} catch (Exception e) {
				e.printStackTrace();
			}

			counter = 0;
		}
	}

}
