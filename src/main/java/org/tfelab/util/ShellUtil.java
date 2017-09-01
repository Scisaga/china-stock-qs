/**
 * ShellUtil.java
 *
 * @author karajan
 * @date 下午6:12:50
 */
package org.tfelab.util;

import java.io.*;

/**
 * @author karajan
 * @date 2015年3月4日 下午6:12:50
 *
 */
public class ShellUtil {

	static BufferedReader in;
	static FileOutputStream out;

	public static void main(String[] args) throws InterruptedException {

		if (args.length != 4) {

			System.err.println("Incorrect argurment number.");
			System.exit(-1);
		}

		String app_name = args[0];
		String ver = args[1];
		String src_folder = args[2];
		String dist_folder = args[3];

		File root = new File(src_folder);
		File[] files = root.listFiles();
		for (File file : files) {
			if (!file.isDirectory()) {

				String fileName = file.getName();
				System.out.println("Proc: " + fileName);

				String src = getFile(src_folder + "/" + fileName);
				src = src.replaceAll("\\{version\\}", ver);
				src = src.replaceAll("\\{app_name\\}", app_name);

				writeFile(dist_folder + "/" + fileName, src);
			}
		}
	}

	/**
	 * 读取文件
	 * @param filePath
	 * @return
	 */
	public static String getFile(String filePath) {

		BufferedReader in;
		String src = "";

		try {
			in = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(filePath),
							"UTF-8"
					)
			);

			String line = null;
			while ((line = in.readLine()) != null) {

				src += line + "\n";
			}

			in.close();

		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return src;
	}

	public static void writeFile(String filePath, String src) {

		try {

			FileOutputStream out;
			File file = new File(filePath);
			file.createNewFile();

			out = new FileOutputStream(file);
			out.write(src.getBytes("utf-8"));

			out.close();

		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
