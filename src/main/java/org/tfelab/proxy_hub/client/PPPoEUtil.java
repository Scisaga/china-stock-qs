package org.tfelab.proxy_hub.client;

import org.tfelab.util.FileUtil;

/**
 * 需要提前安装
 * sudo apt -y install pppoe && sudo chmod 777 /etc/ppp/peers/ -R && sudo chmod 777 /etc/ppp/chap-secrets /etc/ppp/pap-secrets
 */
public class PPPoEUtil {

	private static String provider_path = "/etc/ppp/peers/proxy";
	private static String chap_secrets_path = "/etc/ppp/chap-secrets";
	private static String pap_secrets_path = "/etc/ppp/pap-secrets";

	private static String provider_content = "noipdefault\n" +
			"defaultroute\n" +
			"replacedefaultroute\n" +
			"hide-password\n" +
			"noauth\n" +
			"persist\n" +
			"plugin rp-pppoe.so enp0s3\n" +
			"usepeerdns\n" +
			"user \"{{user}}\"";

	private static String pap_secrets_defaults = "*       hostname        \"\"      *\n" +
			"\n" +
			"guest   hostname        \"*\"     -\n" +
			"master  hostname        \"*\"     -\n" +
			"root    hostname        \"*\"     -\n" +
			"support hostname        \"*\"     -\n" +
			"stats   hostname        \"*\"     -\n";

	private static void createProviderConifg(String user) {
		FileUtil.writeBytesToFile(provider_content.replaceAll("\\{\\{user\\}\\}", user).getBytes(), provider_path);
	}

	private static void addUserSecret(String user, String passwd) {

		String secrets = "\"" + user + "\"\t\\*\t\"" + passwd + "\"";
		FileUtil.writeBytesToFile(secrets.getBytes(), chap_secrets_path);
		FileUtil.writeBytesToFile((pap_secrets_defaults + secrets).getBytes(), pap_secrets_path);
	}

	public static void main(String[] args) {

	}
}
