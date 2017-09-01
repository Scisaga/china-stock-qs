package org.tfelab.util;

import java.io.*;
import java.nio.ByteBuffer;

public class FileUtil {

	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static String readFileByLines(String fileName) {
		String output = "";
		File file = new File(fileName);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				output += tempString + "\n";
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
		return output;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static byte[] readBytesFromFile(String fileName) {
		File file = new File(fileName);
		return readBytesFromFile(file);
	}
	
	public static byte[] readBytesFromFile(File file) {
		try {
			FileInputStream fin = new FileInputStream(file);
			ByteBuffer nbf = ByteBuffer.allocate((int) file.length());
			byte[] array = new byte[1024];
			int offset = 0, length = 0;
			while ((length = fin.read(array)) > 0) {
				if (length != 1024)
					nbf.put(array, 0, length);
				else
					nbf.put(array);
				offset += length;
			}
			fin.close();
			byte[] content = nbf.array();
			return content;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * TODO: 当文件较大时候可能发生问题
	 * @param fileName
	 * @param fileBytes
	 * @return
	 */
	public static boolean writeBytesToFile(byte[] fileBytes, String fileName) {
		
		try {
			
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
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
}
