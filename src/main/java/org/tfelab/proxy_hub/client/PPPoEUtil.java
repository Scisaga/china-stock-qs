package org.tfelab.proxy_hub.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.io.requester.proxy.IpDetector;
import org.tfelab.monitor.sensors.LocalSensor;
import org.tfelab.util.FileUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 需要提前安装
 * sudo apt -y install pppoe && sudo chmod 777 /etc/ppp/peers/ -R && sudo chmod 777 /etc/ppp/chap-secrets /etc/ppp/pap-secrets
 */
public class PPPoEUtil {

	public static final Logger logger = LogManager.getLogger(PPPoEUtil.class.getName());

	private static String provider_path = "/etc/ppp/peers/proxy";
	private static String chap_secrets_path = "/etc/ppp/chap-secrets";
	private static String pap_secrets_path = "/etc/ppp/pap-secrets";

	private static String provider_content = "noipdefault\n" +
			"defaultroute\n" +
			"replacedefaultroute\n" +
			"hide-password\n" +
			"noauth\n" +
			"persist\n" +
			"plugin rp-pppoe.so {{interface}}\n" +
			"usepeerdns\n" +
			"user \"{{user}}\"";

	private static String pap_secrets_defaults = "*       hostname        \"\"      *\n" +
			"\n" +
			"guest   hostname        \"*\"     -\n" +
			"master  hostname        \"*\"     -\n" +
			"root    hostname        \"*\"     -\n" +
			"support hostname        \"*\"     -\n" +
			"stats   hostname        \"*\"     -\n";

	private static String changeMacAddressCmd = "sudo ifconfig {{interface}} down && " +
			"sudo ifconfig {{interface}} hw ether {{mac-address}} && " +
			"sudo ifconfig {{interface}} up";

	public static void createProviderConifg(String user) {
		FileUtil.writeBytesToFile(
				provider_content
						.replaceAll("\\{\\{user\\}\\}", user)
						.replaceAll("\\{\\{interface\\}\\}", getDefaultInterface())
						.getBytes()
				, provider_path);
	}

	public static void addUserSecret(String user, String passwd) {

		String secrets = "\"" + user + "\"\t*\t\"" + passwd + "\"";
		FileUtil.writeBytesToFile(secrets.getBytes(), chap_secrets_path);
		FileUtil.writeBytesToFile((pap_secrets_defaults + secrets).getBytes(), pap_secrets_path);
	}

	/**
	 * TODO
	 * @return
	 */
	private static String getDefaultInterface() {

		Map<String, String> interfaces = new HashMap<>();

		String src = LocalSensor.getLocalShellOutput("ifconfig");
		String[] lines = src.split("\n");

		String interf = null;
		for(String line: lines) {
			String tokens[] = line.split(" {2,}");
			if(tokens[0].length() > 0) {
				interf = tokens[0];
				continue;
			}

			if(interf != null && tokens.length > 1) {
				if(tokens[1].matches("inet addr.*")) {
					String ip = tokens[1].split(":")[1];
					if(!ip.equals("127.0.0.1")) {
						interfaces.put(interf, tokens[1].split(":")[1]);
					}

					interf = null;
					continue;
				}
			}
		}

		for(String key : interfaces.keySet()) {
			return key;
		}

		return null;
	}

	public static void poff() {
		LocalSensor.getLocalShellOutput("poff proxy && ps aux | grep ppp | awk '{print $2}' | xargs kill -9");
	}

	public static String randomMACAddress(){

		Random rand = new Random();
		byte[] macAddr = new byte[6];
		rand.nextBytes(macAddr);

		macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

		StringBuilder sb = new StringBuilder(18);
		for(byte b : macAddr){

			if(sb.length() > 0)
				sb.append(":");

			sb.append(String.format("%02x", b));
		}

		return sb.toString();
	}

	public static String pon() {

		String output = LocalSensor.getLocalShellOutput("pon proxy");
		logger.info(output.replaceAll("\n", "\t"));

		long t1 = System.currentTimeMillis();
		String outerIp = null;
		while (outerIp == null) {
			outerIp = IpDetector.getIp();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		long duration = System.currentTimeMillis() - t1;

		logger.info(outerIp);
		return outerIp;
	}

	public static void changeMACAddress() {
		String cmd = changeMacAddressCmd
				.replaceAll("\\{\\{interface\\}\\}", getDefaultInterface())
				.replaceAll("\\{\\{mac-address\\}\\}", randomMACAddress());

		String cmds[] = cmd.split(" && ");
		for(String c : cmds) {
			String output = LocalSensor.getLocalShellOutput(c);
			logger.info(output);
		}
	}

	public static void main(String[] args) {

//		addUserSecret("11111102664885", "ezkzct");
//		createProviderConifg("11111102664885");
//		poff();
//		pon();
		changeMACAddress();

	}
}
