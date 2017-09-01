package org.tfelab.musical_sharing;

import org.tfelab.io.requester.BasicRequester;
import org.tfelab.io.requester.Task;
import org.tfelab.proxy.ProxyWrapperImpl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class AbuyunTest {

	Timer counterTimer = new Timer();

	volatile int counter = 0;

	public AbuyunTest() throws InterruptedException {

		counterTimer.scheduleAtFixedRate(new CounterThread(), 0L, 1000L);

		for(int i=0; i<10000; i++) {
			Thread.sleep(50);
			new CrawlerThread().start();
		}
	}

	public static ProxyWrapperImpl getProxy() {

		String host = "http-dyn.abuyun.com";
		int port = 9020;

		String user = "H9Y99G067VQSE0YD";
		String passwd = "CA7CA4C4F3DB5DA0";

		ProxyWrapperImpl proxy = new ProxyWrapperImpl(host, port, user, passwd);
		return proxy;
	}


	class CrawlerThread extends Thread {

		public void run() {
			try {
				counter ++;
				Task t = new Task("https://www.baidu.com", null);
				t.setProxy(getProxy());
				BasicRequester.getInstance().fetch(t, 30000);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

		}
	}

	class CounterThread extends TimerTask {
		public void run() {
			System.err.println("Counter: " + counter);
			counter = 0;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		new AbuyunTest();
	}

}
