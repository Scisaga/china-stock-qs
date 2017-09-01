package org.tfelab.io.requester;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.util.FormatUtil;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * 保存SSL网站证书工具类
 * @author karajan@tfelab.org
 * 2016年10月5日 下午9:49:31
 */
public class CertAutoInstaller {
	
	public static final Logger logger = LogManager.getLogger(CertAutoInstaller.class.getName());
	
	static KeyStore ks;
	static char[] passphrase = "changeit".toCharArray();
	
	static {
		
		// 定义系统变量
		System.setProperty("javax.net.ssl.trustStore", "cacerts");
		
		try {
			
			// 读取 KeyStore 并保存至缓存
			File file = new File("cacerts");
			if (file.isFile() == false) {
	            final char SEP = File.separatorChar;
	            final File dir = new File(System.getProperty("java.home")
	                    + SEP + "lib" + SEP + "security");
	            file = new File(dir, "jssecacerts");
	            if (file.isFile() == false) {
	                file = new File(dir, "cacerts");
	            }
	        }
			
			logger.info("Loading KeyStore {}...", file);
			
			InputStream in = file.isFile() ? new FileInputStream(file) : null;
			
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(in, passphrase);
			in.close();
		} catch (Exception e) {
			logger.error(e);
		}
	
	}
	
	/**
	 * 自定义TrustManager 用于接受和保存SSL站点证书
	 * @author karajan@tfelab.org
	 * 2016年10月5日 下午9:50:43
	 */
	private static class SavingTrustManager implements X509TrustManager {

		private final X509TrustManager tm;
		private X509Certificate[] chain;

		SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		@Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
            // throw new UnsupportedOperationException();
        }

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}
	
	/**
	 * 获取SSLSocketFactory
	 * 采集线程中 打开HttpsURLConnection 调用
	 * 不能使用默认的SSLSocketFactory 否则运行时保存的SSL证书将不起作用
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws KeyManagementException
	 */
	public static SSLSocketFactory getSSLFactory() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, KeyManagementException {
	
		SSLContext context = SSLContext.getInstance("TLS");
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
		
		context.init(null, new TrustManager[] { tm }, null);
		
		return context.getSocketFactory();
	}
	
	/**
	 * 保存站点SSL证书方法
	 * @param host
	 * @param port
	 * @throws Exception
	 */
	public static void installCert(String host, int port) throws Exception{
		
		SSLContext context = SSLContext.getInstance("TLS");
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
		SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
		
		context.init(null, new TrustManager[] { tm }, null);
		
		SSLSocketFactory factory = context.getSocketFactory();
		
		logger.info("Obtainning certificates from {}:{}....", host, port);
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(15000);
		try {
			logger.info("Starting SSL handshake...");
			socket.startHandshake();
			socket.close();
			logger.info("No errors, certificate is already trusted");
			return;
		} catch (SSLException e) {
			logger.error(e.toString());
		}
	
		X509Certificate[] chain = tm.chain;
		if (chain == null) {
			throw new Exception("Could not obtain server certificate chain");
		}
		
		logger.info("Server sent {} certificate(s):", chain.length);
		
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		
		for (int i = 0; i < chain.length; i++) {
			
			X509Certificate cert = chain[i];
			logger.info("\t{} Subject {}", (i + 1), cert.getSubjectDN());
			logger.info("\t\tIssuer {}", cert.getIssuerDN());
			sha1.update(cert.getEncoded());
			logger.info("\t\tsha1 {}", FormatUtil.byteArrayToHex(sha1.digest()));
			md5.update(cert.getEncoded());
			logger.info("\t\tmd5 {}", FormatUtil.byteArrayToHex(md5.digest()));
			
			String alias = host + "-" + (i + 1);
			//TODO
			ks.setCertificateEntry(alias, cert);
			OutputStream out = new FileOutputStream("cacerts");
			ks.store(out, passphrase);
			out.close();
			
			logger.info("Added certificate to keystore 'cacerts' using alias {}", alias);
		}
		
	}

	public static void addCert(String certfile) throws Exception {

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream certstream = null;
		try {
			certstream = fullStream (certfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Certificate certs =  cf.generateCertificate(certstream);


		ks.setCertificateEntry(certfile, certs);
		OutputStream out = new FileOutputStream("cacerts");
		ks.store(out, passphrase);
		out.close();

	}

	private static InputStream fullStream ( String fname ) throws IOException {
		FileInputStream fis = new FileInputStream(fname);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return bais;
	}

	/**
	 * 测试方法
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		addCert("FiddlerRoot.cer");
	}

}