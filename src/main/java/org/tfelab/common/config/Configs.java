package org.tfelab.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Luke on 1/28/16. 
 * mailto:stormluke1130@gmail.com
 * 读取classpath下配置文件 config.json
 */
public class Configs {
	
	public static final Config base = ConfigFactory.load();
	
	public static final Config main;
	
	static {
		
		File file = new File("config.json");
		
		if(file.exists()){
			main = ConfigFactory.parseFile(file).withFallback(base);
			
		} else {
			InputStream stream = Configs.class.getClassLoader().getResourceAsStream("config.json");
			main = ConfigFactory.parseReader(new InputStreamReader(stream)).withFallback(base);
		}
		
	}

	public static final Config dev = main.getConfig("dev").withFallback(main);
}
