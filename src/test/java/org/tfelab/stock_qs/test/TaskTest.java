package org.tfelab.stock_qs.test;

import org.junit.Assert;
import org.junit.Test;
import org.tfelab.io.requester.Task;
import org.tfelab.io.requester.account.AccountWrapper;
import org.tfelab.io.requester.account.AccountWrapperImpl;
import org.tfelab.io.requester.proxy.ProxyWrapperImpl;
import org.tfelab.stock_qs.Crawler;
import org.tfelab.stock_qs.proxy.ProxyManager;
import org.tfelab.stock_qs.task.TimeQuoteExtractTask;
import org.tfelab.stock_qs.task.TransactionExtractTask;

public class TaskTest {
	@Test
	public void createSimpleTask() throws Exception {

		Task t = new Task("http://www.baidu.com");
		Task t_ = new Task("http://www.baidu.com");
		Assert.assertNotEquals(t.getId(), t_.getId());

		t.setProxyWrapper(new ProxyWrapperImpl("127.0.0.1", 8888, null, null));
		t.setAccount(new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g));
		t.insert();

	}

	@Test
	public void deserializeTask() throws Exception {

		Task t = Task.getTask("bf8f6cfe8ead5cf6eb11699a5c73294e");

		Assert.assertEquals("127.0.0.1", t.getProxyWrapper().getHost());
		Assert.assertEquals(ProxyManager.abuyun_g, t.getAccountWrapper().getProxyGroup());
	}

	@Test
	public void getSingleTimeQuoteTask() throws InterruptedException {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.aliyun_g);


		Task t = TimeQuoteExtractTask.generateTask("sh600036", "2017-01-03", accountWrapper);
		Crawler.getInstance().addTask(t);
		Thread.sleep(100000);
	}

	@Test
	public void getSingleTransactionTask() throws InterruptedException {

		AccountWrapper accountWrapper = new AccountWrapperImpl().setProxyGroup(ProxyManager.abuyun_g);

		//Crawler crawler = new Crawler();

		Task t = TransactionExtractTask.generateTask("sh600036", "2017-09-15", 1, accountWrapper);
		try {
			t.insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//crawler.addTask(t);
		Thread.sleep(100000);
	}

}
