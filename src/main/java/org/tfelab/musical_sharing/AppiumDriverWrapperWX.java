package org.tfelab.musical_sharing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.tfelab.util.FileUtil;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.RemoteFile;
import se.vidstige.jadb.managers.PackageManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AppiumDriverWrapperWX {

	private static final Logger logger = LogManager.getLogger(AppiumDriverWrapperWX.class.getName());

	static String data = "";

	public static String genyShellPath = "C:\\Progra~1\\Genymobile\\Genymotion\\genyshell.exe";


	public volatile boolean running = true;

	String host = "10.0.0.37";
	BrowserMobProxy proxy;
	int proxyPort;

	AppiumDriverLocalService service;
	URL serviceUrl;
	AndroidDriver<MobileElement> driver;

	// TODO
	int genyDeviceId = 0;
	String city;
	String region;
	List<GpsRegion> location_visited = new ArrayList<>();

	public AppiumDriverWrapperWX() {

	}

	/**
	 *
	 */
	public void startProxy() {

		proxy = new BrowserMobProxyServer();
		proxy.setTrustAllServers(true);
		/*proxy.setMitmManager(ImpersonatingMitmManager.builder().trustAllServers(true).build());*/
		proxy.start(0);
		proxyPort = proxy.getPort();

		logger.info("Proxy started @port {}", proxyPort);

		proxy.addResponseFilter(new ResponseFilter() {
			@Override
			public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {

				if(messageInfo.getOriginalUrl().contains("b.dian.so/lhc/2.0/h5/shop/gets")){


					ObjectMapper mapper = new ObjectMapper();
					JsonNode json = null;
					try {
						json = mapper.readTree(contents.getTextContents());
					} catch (IOException e) {
						e.printStackTrace();
					}

					for(JsonNode subNode : json.get("data").get("shops")) {
						String line = subNode.get("id").asText() + ",\""
								+ subNode.get("address").asText() + "\","
								+ subNode.get("latitude").asText() + ","
								+ subNode.get("longitude").asText() + ","
								+ subNode.get("shopName").asText() + ","
								+ city + ","
								+ region + ","
								+ subNode.get("picUrl").asText() + ","
								+ subNode.get("borrowNum").asInt() + ","
								+ subNode.get("returnNum").asInt() + ","
								+ subNode.get("showBox").asBoolean() + ","
								+ subNode.get("shopDeviceType").asInt() + ","
								+ subNode.get("tableSupport").asBoolean() + ","
								+ subNode.get("shopDeviceType").asInt() + ","
								+ subNode.get("shopDeviceType").asInt() + "\n";

						data = data + line;
					}

					System.err.println(data.length());

				}
			}
		});

	}

	/**
	 *
	 * @param mobileSerial
	 * @param ssid
	 * @param key
	 */
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

					Thread.sleep(500);

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


				}
			}


		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 *
	 * @param genyDeviceId
	 * @param latitude
	 * @param longitude
	 * @throws IOException
	 */
	public void setGps(int genyDeviceId, double latitude, double longitude) throws IOException {

		execGenyShell(genyDeviceId, "gps setlatitude " + latitude);
		execGenyShell(genyDeviceId, "gps setlongitude " + longitude);
	}

	/**
	 *
	 * @param genyDeviceId
	 * @param cmd
	 * @throws IOException
	 */
	public void execGenyShell(int genyDeviceId, String cmd) throws IOException {

		Process proc = Runtime.getRuntime().exec(genyShellPath + " -c \"" + cmd + "\"");

		BufferedReader in = new BufferedReader(
				new InputStreamReader(proc.getInputStream()));
		String line = null;

		while ((line = in.readLine()) != null) {
			logger.info(line);
		}

		in.close();
		proc.destroy();
	}

	/**
	 *
	 * @param d
	 * @param command
	 * @param args
	 * @throws IOException
	 * @throws JadbException
	 */
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
	 * @param udid
	 */
	public void startAppniumServer(String udid) {

		DesiredCapabilities serverCapabilities = new DesiredCapabilities();
		serverCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		serverCapabilities.setCapability(MobileCapabilityType.UDID, udid);
		serverCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 60);
		serverCapabilities.setCapability(AndroidMobileCapabilityType.CHROMEDRIVER_EXECUTABLE,
				"C:\\App\\chromedriver\\2.31\\chromedriver.exe");
		serverCapabilities.setCapability(AndroidMobileCapabilityType.RECREATE_CHROME_DRIVER_SESSIONS, "true");

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
				.withArgument(GeneralServerFlag.SESSION_OVERRIDE)
				.build();

		service.start();

		serviceUrl = service.getUrl();
	}

	/**
	 *
	 * @return
	 */
	public void startAppiumDriver(String udid, String appPackage, String appActivity, String webViewAndroidProcessName) throws MalformedURLException {

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
		capabilities.setCapability("noReset", true);
		capabilities.setCapability("noSign", true);

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);
		/*capabilities.setCapability(MobileCapabilityType.APP, app.getAbsolutePath());
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, MobilePlatform.ANDROID);*/
		driver = new AndroidDriver<>(serviceUrl, capabilities);

	}

	boolean visit(GpsRegion gr) {

		if(this.location_visited.contains(gr)) {
			return false;
		} else {

			double distance = 0;

			for(GpsRegion gr_ : location_visited) {
				distance = Math.sqrt( Math.pow(gr.lat-gr_.lat, 2) + Math.pow(gr.lat-gr_.lat, 2) );
				if(distance < 0.01) return false;
			}

			this.location_visited.add(gr);
			return true;
		}

	}

	/**
	 *
	 */
	public void run() {

		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//*[@text='Discover']")
		));

		driver.findElementByXPath("//*[@text='Discover']").click();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		driver.findElementByXPath("//*[@text='Mini Programs']").click();

		wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//*[@text='Mini Programs Nearby']")
		));

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		driver.findElementByXPath("//*[@text='小电充电']").click();

		wait.until(ExpectedConditions.presenceOfElementLocated(
				By.id("iw")
		));

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/*driver.context("WEBVIEW_com.tencent.mm:appbrand0");
		driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);*/

		for(GpsRegion gpsRegion : loadGps()) {

			System.err.println(gpsRegion.region);
			if(!visit(gpsRegion)) {
				System.err.println("离其他坐标点过近, 丢弃. ");
				continue;
			}

			try {
				setGps(genyDeviceId, gpsRegion.lat, gpsRegion.lng);
				city = gpsRegion.city;
				region = gpsRegion.region;

			} catch (IOException e) {
				e.printStackTrace();
			}

//			driver.findElementById("iw").click();

			TouchAction swipe = new TouchAction(driver).press(140, 2100)
					.waitAction(100).release();
			swipe.perform();

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 *
	 */
	public void close() {

		try {
			driver.quit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		proxy.stop();
		service.stop();
	}

	/**
	 *
	 */
	static class GpsRegion {
		String city;
		String region;
		double lat;
		double lng;
		GpsRegion(String city, String region, double lat, double lng) {
			this.city = city;
			this.region = region;
			this.lat = lat;
			this.lng = lng;
		}
	}

	/**
	 *
	 * @return
	 */
	public static List<GpsRegion> loadGps() {

		List<GpsRegion> regions = new ArrayList<>();

		File file = new File("gps.txt");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				String[] items = tempString.split("\t");
				if(items.length == 5) {
					regions.add(new GpsRegion(
							items[0],
							items[2],
							Double.valueOf(items[3]),
							Double.valueOf(items[4])
					));
				}

			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		return regions;
	}

	/**
	 *
	 */
	public static void setupCSV() {
		String header = "id,address,latitude,longitude,shopName,city,region,picUrl,borrowNum,returnNum,showBox,shopDeviceType,tableSupport,pTotalAmount,pavailableAmount\n";
		data += header;
	}

	/**
	 *
	 * @param fileBytes
	 * @param fileName
	 * @return
	 */
	public static boolean writeBytesToFile(byte[] fileBytes, String fileName) {

		try {

			byte[] bom = { (byte) 239, (byte) 187, (byte) 191 };
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
			bos.write(bom);
			bos.write(fileBytes);
			bos.flush();
			bos.close();
			return true;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 *
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		setupCSV();

		String mobileSerial = "192.168.52.101:5555";
		int genyDeviceId = 0;
		String appPackage = "com.tencent.mm";
		String appActivity = ".ui.LauncherUI";
		String webViewAndroidProcessName = "com.tencent.mm:appbrand0";

		AppiumDriverWrapperWX wrapper = new AppiumDriverWrapperWX();
		wrapper.startProxy();

		wrapper.setUpAndroidProxy(mobileSerial, "WiredSSID", null);
		//wrapper.setUpProxy(mobileSerial, "KMP_6th_Element", "TfeLAB2@16");
		wrapper.startAppniumServer(mobileSerial);
		wrapper.startAppiumDriver(mobileSerial, appPackage, appActivity, webViewAndroidProcessName);
		wrapper.run();
		wrapper.close();

		System.err.println(data.length());
		writeBytesToFile(data.getBytes(), "jiedian-2.csv");
		System.err.println("Done.");
	}


}
