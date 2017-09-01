package org.tfelab.musical_sharing;

import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.netty.handler.codec.http.*;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tfelab.musical_sharing.model.AccessConfidential;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.RemoteFile;
import se.vidstige.jadb.managers.PackageManager;
import sun.misc.Request;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class AppiumDriverWrapper extends Thread {

	private static final Logger logger = LogManager.getLogger(AppiumDriverWrapper.class.getName());

	public volatile boolean running = true;

	static File nodeJsExecutable = new File("C:\\Program Files\\nodejs\\node.exe");
	static File appniumMainJsFile = new File("C:\\Users\\karajan\\AppData\\Local\\Programs\\appium-desktop\\resources\\app");

	String host = "10.0.0.56";
	BrowserMobProxy proxy;
	int proxyPort;

	AppiumDriverLocalService service;
	URL serviceUrl;
	AndroidDriver<MobileElement> driver;

	String webViewAndroidProcessName = "com.tencent.mm:appbrand0";
	String appPackage = "com.tencent.mm";
	String appActivity = ".ui.LauncherUI";

	static String H60X_UDID = "F8UDU15428003335";
	static String H60_UDID = "DU2SSE152D062005";
	static String XM4A_UDID = "47734067d140";
	static String S6_UDID = "192.168.52.101:5555";

	public AppiumDriverWrapper() {

	}

	public void startProxy() {

		proxy = new BrowserMobProxyServer();
		proxy.setTrustAllServers(true);
		/*proxy.setMitmManager(ImpersonatingMitmManager.builder().trustAllServers(true).build());*/
		proxy.start(0);
		proxyPort = proxy.getPort();

		logger.info("Proxy started @port {}", proxyPort);

		RequestFilter filter = new RequestFilter() {
			@Override
			public HttpResponse filterRequest(HttpRequest request, HttpMessageContents contents, HttpMessageInfo messageInfo) {

				if(messageInfo.getOriginalUrl().contains("https://live.direct.ly//rest/lives/v1/discovery/channels/latest?viewedId=")){

					logger.info(messageInfo.getOriginalUrl());

					AccessConfidential ac = new AccessConfidential(
							messageInfo.getOriginalRequest().headers().get("X-Request-ID"),
							messageInfo.getOriginalRequest().headers().get("X-Request-Info5"),
							messageInfo.getOriginalRequest().headers().get("X-Request-Sign5"),
							messageInfo.getOriginalRequest().headers().get("Authorization"),
							messageInfo.getOriginalRequest().headers().get("build"),
							0L
					);

					try {
						ac.insert();
					} catch (Exception e) {
						logger.error("Error insert AccessConfidential, ", e);
					}
				}

				return null;
			}
		};

		proxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));

		proxy.addResponseFilter(new ResponseFilter() {
			@Override
			public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {


				if(messageInfo.getOriginalUrl().contains("https://live.direct.ly//rest/lives/v1/discovery/channels/latest?viewedId=")){

					String uid = contents.getTextContents().replaceAll("^.+?\"id\":", "").replaceAll(",.+?$", "");

					try {

						if(!Crawler.userIds.contains(uid) && !Crawler.userIdQueueSet.contains(uid)) {
							Crawler.userIdQueue.put(uid);
							Crawler.userIdQueueSet.add(uid);
						}

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	public void setUpAndroidProxy(String mobileSerial, String ssid, String key) {

		try {

			JadbConnection jadb = new JadbConnection();
			List<JadbDevice> devices = jadb.getDevices();
			for(JadbDevice d : devices) {
				if(d.getSerial().equals(mobileSerial)) {

					try {
						new PackageManager(d).install(new File("proxy-setter-debug-0.2.apk"));
					} catch (Exception e) {
						logger.error("proxy-setter already installed.");
					}

					d.push(new File("ca-certificate-rsa.crt"),
							new RemoteFile("/sdcard/_certs/ca-certificate-rsa.crt"));

					execShell(d,"am", "start",
							"-n", "tk.elevenk.proxysetter/.MainActivity",
							"-e", "ssid", ssid,
							"-e", "clear", "true");

					Thread.sleep(2000);

					if(key == null) {
						execShell(d,"am", "start",
								"-n", "tk.elevenk.proxysetter/.MainActivity",
								"-e", "host", host,
								"-e", "port", String.valueOf(proxyPort),
								"-e", "ssid", ssid,
								"-e", "reset-wifi", "true");
					} else {
						execShell(d,"am", "start",
								"-n", "tk.elevenk.proxysetter/.MainActivity",
								"-e", "host", host,
								"-e", "port", String.valueOf(proxyPort),
								"-e", "ssid", ssid,
								"-e", "key", key,
								"-e", "reset-wifi", "true");
					}

					Thread.sleep(2000);


				}
			}


		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void execShell(JadbDevice d, String command, String... args) throws IOException, JadbException {
		InputStream is = d.executeShell(command, args);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n"); //appende a new line
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info(builder.toString());
	}

	/**
	 *
	 */
	public void startAppnium(String udid) {

		DesiredCapabilities serverCapabilities = new DesiredCapabilities();
		serverCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		serverCapabilities.setCapability(MobileCapabilityType.UDID, udid);
		serverCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 60);
//		serverCapabilities.setCapability(AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE,
//				"C:\\App\\chromedriver\\2.28\\chromedriver.exe");

		service = new AppiumServiceBuilder()
				/*.usingDriverExecutable(nodeJsExecutable)
				.withAppiumJS(appniumMainJsFile)*/
				.withCapabilities(serverCapabilities)
				/*.withIPAddress(ip)*/
				.usingAnyFreePort()
				/*.usingPort(port)
				.withArgument(AndroidServerFlag.CHROME_DRIVER_PORT, "")
				.withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, "")
				.withArgument(AndroidServerFlag.SELENDROID_PORT, "")
				.withLogFile(new File("appium.log"))*/
				.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				.build();

		service.start();

		serviceUrl = service.getUrl();
	}

	/**
	 *
	 * @return
	 */
	public void startDriver(String udid, String appPackage, String appActivity, String webViewAndroidProcessName) throws MalformedURLException {

		ChromeOptions options = new ChromeOptions();
		if (webViewAndroidProcessName != null) {
			options.setExperimentalOption("androidProcess", webViewAndroidProcessName);
		}

		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("app", "");
		capabilities.setCapability("appPackage", appPackage);
		capabilities.setCapability("appActivity", appActivity);
		capabilities.setCapability("fastReset", "false");
		capabilities.setCapability("fullReset", "false");
		capabilities.setCapability("noReset", "true");

		capabilities.setCapability(ChromeOptions.CAPABILITY, options);

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);
		/*capabilities.setCapability(MobileCapabilityType.APP, app.getAbsolutePath());
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);*/
		driver = new AndroidDriver<>(serviceUrl, capabilities);
	}

	public void run() {

		MobileElement v_view_pager = driver.findElement(By.className("com.zhiliaoapp.musically.customview.VerticalViewPager"));
		TouchAction swipe = new TouchAction(driver).press(v_view_pager, 384, 640 - 200)
				.waitAction(1000).moveTo(v_view_pager, 384, 640 + 200).release();
		swipe.perform();

		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("abl")
		));

		Exception e = null;

		while(running && e == null) {

			try {
				new TouchAction(driver)
						.tap(driver.findElement(By.id("abl"))).perform();

				wait.until(ExpectedConditions.presenceOfElementLocated(By
						.id("abw")
				));

				new TouchAction(driver)
						.tap(driver.findElement(By.id("e0"))).perform();

				wait.until(ExpectedConditions.presenceOfElementLocated(By
						.id("abl")
				));

				MobileElement v_view_pager_ = driver.findElement(By.className("com.zhiliaoapp.musically.customview.VerticalViewPager"));
				TouchAction swipe_ = new TouchAction(driver).press(v_view_pager_, 384, 640 + 200)
						.waitAction(1000).moveTo(v_view_pager_, 384, 640 - 200).release();
				swipe_.perform();

				Thread.sleep(10000);
			} catch (Exception ex) {
				e = ex;
				ex.printStackTrace();
			}
		}

	}

	public static void main(String[] args) throws MalformedURLException {

		String appPackage = "com.zhiliaoapp.musically";
		String appActivity = ".activity.SplashActivity";
		String mobileSerial = "192.168.56.101:5555";

		AppiumDriverWrapper wrapper = new AppiumDriverWrapper();
		wrapper.startProxy();
		wrapper.setUpAndroidProxy(mobileSerial, "WiredSSID", null);


		wrapper.startAppnium("192.168.56.101:5555");
		wrapper.startDriver("192.168.56.101:5555", appPackage, appActivity, null);
		wrapper.start();

	}

}
