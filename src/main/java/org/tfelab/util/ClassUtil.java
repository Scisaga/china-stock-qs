package org.tfelab.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassUtil {
	
	private static String src_path = "customized_class";

	public final static Logger logger = LogManager.getLogger(ClassUtil.class.getName());
	
	/**
	 * 通过源文件动态加载Java类
	 * @param src
	 */
	public static boolean genSrcFile(String className, String src) {

		logger.info(className);

		// Save source in .java file.
		File root = new File(src_path);
		File sourceFile = new File(root, className.replaceAll("\\.", "/") + ".java");
		logger.info(sourceFile.getAbsolutePath());

		sourceFile.delete();
		sourceFile.getParentFile().mkdirs();

		try {

			FileWriter writer = new FileWriter(sourceFile);
			writer.write(src);
			writer.close();

		} catch (IOException e) {
			logger.error(e);
		}

		// Compile source file.
		return compileSource(sourceFile);
	}
	
	/**
	 * 编译源代码
	 * @param folder_path
	 * @param sourceFile
	 * @return
	 */
	public static boolean compileSource(File sourceFile) {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		try {

			FileOutputStream err = new FileOutputStream("compile_err.txt");

			int returnCode = compiler.run(null, null, err, sourceFile.getAbsolutePath());
			if (returnCode == 0) {
				logger.info(sourceFile.getAbsolutePath() + " Done.");
				return true;
			} else {
				logger.info(sourceFile.getAbsolutePath() + " Failed. Return Code: " + returnCode);
				return false;
			}

		} catch (FileNotFoundException e) {
			logger.error(e);
		}

		return false;
	}
	
	/**
	 * Load customized class via system class loader
	 */
	public static boolean loadClass() {

		File root = new File(src_path);

		ClassLoader cl = ClassLoader.getSystemClassLoader();

		try {

			if (cl instanceof URLClassLoader) {

				logger.info(root.toURI().toURL());

				URLClassLoader ul = (URLClassLoader) cl;
				// addURL is a protected method, but we can use reflection to call it
				Class<?>[] paraTypes = new Class[1];
				paraTypes[0] = URL.class;
				Method method = URLClassLoader.class.getDeclaredMethod("addURL", paraTypes);

				// change access to true, otherwise, it will throw exception
				method.setAccessible(true);
				method.invoke(ul, root.toURI().toURL());

				return true;
			} else {
				return false;
			}

		} catch (MalformedURLException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {

			logger.error(e);
			return false;
		}
	}
	
	/**
	 * 删除自定义类
	 * @param className
	 */
	public static void removeClass(String className) {

		File root = new File(src_path);
		File sourceFile = new File(root, className.replaceAll("\\.", "/") + ".java");
		sourceFile.delete();

		sourceFile = new File(root, className.replaceAll("\\.", "/") + ".class");
		sourceFile.delete();
	}
	
	public static void removeAllClass() {
		File root = new File(src_path); 
		root.delete();
	}
}
