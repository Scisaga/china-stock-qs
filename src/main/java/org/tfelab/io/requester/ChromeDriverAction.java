package org.tfelab.io.requester;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.tfelab.common.config.Configs;

import static org.tfelab.io.requester.ChromeDriverAgent.*;

/**
 * Created by karajan on 2017/6/3.
 */
public class ChromeDriverAction {
	public boolean run(ChromeDriver driver) throws Exception {
		return true;
	}
}

/**
 * 输入框填值
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:47:31
 */
class SetValueAction extends ChromeDriverAction {

	public String inputPath;
	public String value;

	public SetValueAction(String inputPath, String value) {
		this.inputPath = inputPath;
		this.value = value;
	}

	public boolean run(ChromeDriver driver) {

		//WebElement el = driver.findElement(By.cssSelector(inputPath));
		try {
			WebElement el = getElementWait(driver, inputPath);

			if(el == null) {
				logger.info("{} not found.", inputPath);
				return false;
			}
			el.clear();
			el = null;
			el = driver.findElement(By.cssSelector(inputPath));
			el.sendKeys(value);
			return true;
		} catch (org.openqa.selenium.TimeoutException e) {

			return true;
		}
	}
}

class ClearCacheAction extends ChromeDriverAction {

	public boolean run(ChromeDriver driver) {

		//WebElement el = driver.findElement(By.cssSelector(inputPath));
		try {
			driver.get("chrome://settings-frame/clearBrowserData");
			new ClickAction("#clear-browser-data-commit").run(driver);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
}

/**
 * 点击
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:47:18
 */
class ClickAction extends ChromeDriverAction {

	public String elementPath;

	public ClickAction(String elementPath) {
		this.elementPath = elementPath;
	}

	public boolean run(ChromeDriver driver) throws InterruptedException {
		WebElement we = driver.findElement(By.cssSelector(elementPath));
		if(we != null) {
			try {
				we.click();
			} catch (org.openqa.selenium.TimeoutException e) {
				//driver.navigate().refresh();
			}

			Thread.sleep(5000);
			return true;
		} else {
			logger.info("{} not found.", elementPath);
			return false;
		}
	}
}

/**
 * 执行脚本
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:48:13
 */
class ExecAction extends ChromeDriverAction {

	public String script;

	public ExecAction(String script) {
		this.script = script;
	}

	public boolean run(ChromeDriver driver) {
		try {
			if(script != null & script.length() > 0)
				driver.executeScript(script);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}

/**
 * 刷新页面
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:48:30
 */
class RefreshAction extends ChromeDriverAction {
	public boolean run(ChromeDriver driver) {
		driver.navigate().refresh();
		return true;
	}
}

/**
 * 浏览页面
 * @author karajan@tfelab.org
 * 2017年3月21日 下午8:48:52
 */
class RedirectAction extends ChromeDriverAction {

	public String url;

	public RedirectAction(String url) {
		this.url = url;
	}

	public boolean run(ChromeDriver driver) throws InterruptedException {

		try {
			driver.get(url);
			Thread.sleep(5000);
		} catch (org.openqa.selenium.TimeoutException e) {
			//driver.navigate().refresh();
		}

		return true;
	}
}

/**
 * 滚轮事件
 */
class ScrollAction extends ChromeDriverAction {

	public String value;

	public ScrollAction(String value) {
		this.value = value;
	}

	public boolean run(ChromeDriver driver) {
		try {
			String setscroll = "document.documentElement.scrollTop=" + value;
			driver.executeScript(setscroll);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}