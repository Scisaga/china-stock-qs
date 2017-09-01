package org.tfelab.io.requester;

/**
 * 
 * @author karajan@tfelab.org
 * 2017年3月17日 下午8:50:08
 */
public class Requester {
	
	public void fetch(Task task) {}
	
	public void fetch(Task task, long timeout) {}
	
	public void close() {}
	
	public static Requester getRequester(String name){
		
		if(name == null) {
			return BasicRequester.getInstance();
		}
		else if (name.length() == 0) {
			return BasicRequester.getInstance();
		}
		else if (name.equals(BasicRequester.class.getSimpleName())) {
			return BasicRequester.getInstance();
		}
		else if (name.equals(ChromeDriverRequester.class.getSimpleName())) {
			return ChromeDriverRequester.getInstance();
		}
		else {
			return BasicRequester.getInstance();
		}
	}
	
	public static void closeAll() {
		if(BasicRequester.instance != null) {
			BasicRequester.getInstance().close();
		}
		if(ChromeDriverRequester.instance != null) {
			ChromeDriverRequester.getInstance().close();
		}
	}

}
