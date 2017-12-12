package org.tfelab.proxy_hub.tls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.util.Networks;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.*;

/**
 * Generate ca root key store.
 * First, generate one public-private key pair, then create a X509 certificate, including the generated public key.
 * JDK do not have an open api for building X509 certificate, so we use Bouncy Castle here.
 *
 * @author Liu Dong
 */
public class KeyStoreGenerator {

	public static final Logger logger = LogManager.getLogger(KeyStoreGenerator.class.getName());

    private static final KeyStoreGenerator instance = new KeyStoreGenerator();

	private static final String KEYGEN_ALGORITHM = "RSA";

	private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";

	private static final int ROOT_KEYSIZE = 2048;

	private static final int KEYSIZE = 2048;

    private KeyStoreGenerator() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

    public static KeyStoreGenerator getInstance() {
        return instance;
    }

	/**
	 * 生成keypair
	 * @param keySize
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 */
	public static KeyPair generateKeyPair(int keySize)
			throws NoSuchAlgorithmException, NoSuchProviderException {
		KeyPairGenerator generator = KeyPairGenerator
				.getInstance(KEYGEN_ALGORITHM);
		SecureRandom secureRandom = SecureRandom
				.getInstance(SECURE_RANDOM_ALGORITHM);
		generator.initialize(keySize, secureRandom);
		return generator.generateKeyPair();
	}

	/**
	 * 生成初始随机数
	 * @return
	 */
	public static long initRandomSerial() {
		final Random rnd = new Random();
		rnd.setSeed(System.currentTimeMillis());
		// prevent browser certificate caches, cause of doubled serial numbers
		// using 48bit random number
		long sl = ((long) rnd.nextInt()) << 32 | (rnd.nextInt() & 0xFFFFFFFFL);
		// let reserve of 16 bit for increasing, serials have to be positive
		sl = sl & 0x0000FFFFFFFFFFFFL;
		return sl;
	}

	/**
	 * Generate a root ca key store.
	 * The key store is stored by p#12 format, X.509 Certificate encoded in der format
	 *
	 * @param password 证书密码
	 * @param validityDays 证书有效天数
	 * @return
	 * @throws Exception
	 */
    public KeyStore generateRootCert(char[] password, int validityDays) throws Exception {

		/**
		 * 生成根证书公钥和私钥
		 */
        KeyPair keypair = generateKeyPair(ROOT_KEYSIZE);


		/**
		 * 准备生成证书所需要的信息
		 */
		Security.addProvider(new BouncyCastleProvider());

		X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
		nameBuilder.addRDN(BCStyle.CN, "Proxy"); // Common Name
		nameBuilder.addRDN(BCStyle.O, "Proxy"); // Organization
		nameBuilder.addRDN(BCStyle.OU, "Proxy"); // Organization Unit Name
		nameBuilder.addRDN(BCStyle.L, "Beijing"); // Location
		nameBuilder.addRDN(BCStyle.ST, "Beijing"); // States
		nameBuilder.addRDN(BCStyle.C, "CN"); // Country

		X500Name issuer = nameBuilder.build();
		X500Name subject = issuer;

		Date startDate = DateTime.now().minusDays(100).toDate();
		Date expireDate = DateTime.now().plusDays(validityDays).toDate();

        // 证书Builder
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				issuer,
                BigInteger.valueOf(initRandomSerial()),
                startDate,
				expireDate,
				subject,
                keypair.getPublic()
        );

        builder.addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyIdentifier(keypair.getPublic()));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true)); // 生成CA证书
        KeyUsage usage = new KeyUsage(
        		KeyUsage.keyCertSign |
				KeyUsage.digitalSignature |
				KeyUsage.keyEncipherment |
				KeyUsage.dataEncipherment |
				KeyUsage.cRLSign); // 证书的适用范围定义
        builder.addExtension(Extension.keyUsage, false, usage);

        ASN1EncodableVector purposes = new ASN1EncodableVector();
        purposes.add(KeyPurposeId.id_kp_serverAuth); // 用于服务端证书验证
        purposes.add(KeyPurposeId.id_kp_clientAuth); // 用户客户端证书验证
        purposes.add(KeyPurposeId.anyExtendedKeyUsage);
        builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

		/**
		 * 生成根证书
		 */
		X509Certificate cert = signCertificate(builder, keypair.getPrivate());
        cert.checkValidity(new Date()); // 验证证书有效
        cert.verify(keypair.getPublic()); // 验证公钥匹配


		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);

        X509Certificate[] chain = new X509Certificate[]{cert};

		/**
		 * 将根证书保存在KeyStore中
		 */
		keyStore.setEntry("Proxy",
				new KeyStore.PrivateKeyEntry(keypair.getPrivate(), chain),
                new KeyStore.PasswordProtection(password)
		);

        return keyStore;
    }

	public KeyStore generateServerCert(KeyStore rootKeyStore, char[] rootCertPassword, String host, int validityDays, char[] passwd) throws Exception {

		Enumeration<String> aliases = rootKeyStore.aliases();
		String alias = aliases.nextElement();

		logger.debug("Loading CA certificate/private by alias {}", alias);

		Key key = rootKeyStore.getKey(alias, rootCertPassword);
		Objects.requireNonNull(key, "Specified key of the KeyStore not found!");

		RSAPrivateCrtKey caKey = (RSAPrivateCrtKey) key;

		/**
		 * 获取根证书
		 */
		X509Certificate rootCert = (X509Certificate) rootKeyStore.getCertificate(alias);
		Objects.requireNonNull(rootCert, "Specified certificate of the KeyStore not found!");

		logger.debug("Successfully loaded CA key and certificate. CA DN is {}", rootCert.getSubjectDN().getName());

		rootCert.verify(rootCert.getPublicKey()); // 公钥验证
		logger.debug("Successfully verified CA certificate with its own public key.");

		KeyPair keypair = generateKeyPair(KEYSIZE);

		Date startDate = DateTime.now().minusDays(1).toDate();
		Date expireDate = DateTime.now().plusDays(validityDays).toDate();

		/**
		 * Set Subject Alternative Name Extension
		 */
		int hostType = Networks.getHostType(host);
		GeneralNames subjectAltName;
		if (hostType == Networks.HOST_TYPE_IPV4 || hostType == Networks.HOST_TYPE_IPV6) {
			subjectAltName = new GeneralNames(new GeneralName(GeneralName.iPAddress, host));
		} else {
			subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, host));
		}

		/**
		 *
		 */
		X500Principal subjectName = new X500Principal("CN=Proxy V3 Certificate, OU=Proxy, O=Proxy, L=Beijing, ST=Beijing, C=CN");

		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
				rootCert.getSubjectX500Principal(),
				BigInteger.valueOf(initRandomSerial()),
				startDate,
				expireDate,
				subjectName,
				keypair.getPublic());

		builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
		builder.addExtension(Extension.subjectKeyIdentifier, false,
				createSubjectKeyIdentifier(keypair.getPublic()));
		builder.addExtension(Extension.basicConstraints, false,
				new BasicConstraints(false));

		X509Certificate cert = signCertificate(builder, caKey);

		cert.checkValidity(new Date());
		cert.verify(rootCert.getPublicKey());

		KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, null);

		X509Certificate[] chain = new X509Certificate[]{cert, rootCert};
		store.setKeyEntry("Proxy client", keypair.getPrivate(), passwd, chain);
		return store;
	}

	/**
	 * 生成 SubjectKeyIdentifier
	 * @param key
	 * @return
	 * @throws IOException
	 */
    private static SubjectKeyIdentifier createSubjectKeyIdentifier(Key key) throws IOException {
        try (ASN1InputStream is = new ASN1InputStream(new ByteArrayInputStream(key.getEncoded()))) {
            ASN1Sequence seq = (ASN1Sequence) is.readObject();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(seq);
            return new BcX509ExtensionUtils().createSubjectKeyIdentifier(info);
        }
    }

	/**
	 * 证书签名
	 * @param certificateBuilder
	 * @param signedWithPrivateKey
	 * @return
	 * @throws OperatorCreationException
	 * @throws CertificateException
	 */
    private static X509Certificate signCertificate(
    		X509v3CertificateBuilder certificateBuilder,
			PrivateKey signedWithPrivateKey)
            throws OperatorCreationException, CertificateException
	{
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(signedWithPrivateKey);

        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(signer));
    }

	/**
	 * 保存根证书
	 * @throws Exception
	 */
	public synchronized void createRootKeyStoreFile() throws Exception {

		File keyStoreFile = new File(ProxyContext.rootKeyStorePath);
		keyStoreFile.getParentFile().mkdirs();

		KeyStore rootKeyStore = generateRootCert(ProxyContext.rootCertPassword, ProxyContext.rootCertValidityDays);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		rootKeyStore.store(bos, ProxyContext.rootKeyStorePassword);

		Files.write(Paths.get(ProxyContext.rootKeyStorePath), bos.toByteArray());
	}

	public KeyStore generateServerCert(String host) throws Exception {

		logger.debug("Loading CA certificate/private key from file {}", ProxyContext.rootKeyStorePath);

		Path keyStorePath = Paths.get(ProxyContext.rootKeyStorePath);
		if(!Files.exists(keyStorePath)) {
			createRootKeyStoreFile();
		}

		KeyStore rootKeyStore = KeyStore.getInstance("PKCS12");
		try (InputStream input = Files.newInputStream(Paths.get(ProxyContext.rootKeyStorePath))) {
			rootKeyStore.load(input, ProxyContext.rootKeyStorePassword);
		}

		return generateServerCert(rootKeyStore, ProxyContext.rootCertPassword, host, ProxyContext.certValidityDays, ProxyContext.certPassword);
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			KeyStoreGenerator.getInstance().createRootKeyStoreFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
