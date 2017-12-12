package org.tfelab.proxy_hub.tls;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tfelab.proxy_hub.common.ProxyContext;
import org.tfelab.proxy_hub.exception.TslContextException;
import org.tfelab.proxy_hub.util.Networks;

import javax.net.ssl.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hold current root cert and cert generator
 *
 * @author Liu Dong
 */
public class SSLContextManager {

	public static final Logger logger = LogManager.getLogger(SSLContextManager.class.getName());

	private static final SSLContextManager instance = new SSLContextManager();

	public static SSLContextManager getInstance() {
		return instance;
	}


    private final ConcurrentHashMap<String, SSLContext> sslContextCache = new ConcurrentHashMap<>();

	SslContextBuilder client_ssl_context_builder;

	private final ConcurrentHashMap<String, SslContextBuilder> sslContextBuilderCache = new ConcurrentHashMap<>();

    private SSLContextManager() {
        this.sslContextCache.clear();
    }

    /**
     *
     * @param host
     * @return
     */
    public SslHandler getServerSslHandler(String host) {
        SSLEngine serverSSLEngine = getSSLContext(host).createSSLEngine();
        serverSSLEngine.setUseClientMode(false);
        return new SslHandler(serverSSLEngine);
    }

    /**
     *
     * @param host
     * @param port
     * @return
     */
    public SslHandler getClientSslHandler(String host, int port) {
        SSLEngine sslEngine = ClientSSLContextFactory.getInstance().get()
                .createSSLEngine(host, port); // using SNI
        sslEngine.setUseClientMode(true);
        return new SslHandler(sslEngine);
    }

    /**
     * Create ssl context for the host
     */
    private synchronized SSLContext getSSLContext(String host) {

        host = Networks.wildcardHost(host);

        return sslContextCache.computeIfAbsent(host, h -> {
            try {
                return createSslContext(h);
            } catch (Exception e) {
                throw new TslContextException(e);
            }
        });
    }

    /**
     *
     * @param host
     * @return
     * @throws Exception
     */
    private SSLContext createSslContext(String host) throws Exception {

        KeyStore keyStore = KeyStoreGenerator.getInstance().generateServerCert(host);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, ProxyContext.keyStorePassword);

        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, null, new SecureRandom());
        return sslContext;
    }

	public SslContext getClientCtx() throws SSLException {

    	if(client_ssl_context_builder == null) {
			client_ssl_context_builder = SslContextBuilder
					.forClient()
					.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
					.applicationProtocolConfig(
							new ApplicationProtocolConfig(
									ApplicationProtocolConfig.Protocol.ALPN,
									ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
									ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
									ApplicationProtocolNames.HTTP_2,
									ApplicationProtocolNames.HTTP_1_1)
					);

			client_ssl_context_builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
		}

		return client_ssl_context_builder.build();
	}

	public SslContext getServerCtx(String host) throws Exception {

		host = Networks.wildcardHost(host);

		SslContextBuilder builder;

		if((builder = sslContextBuilderCache.get(host)) == null) {

			KeyStore keyStore = KeyStoreGenerator.getInstance().generateServerCert(host);

			Enumeration<String> aliases = keyStore.aliases();
			String alias = aliases.nextElement();

			logger.debug("Loading CA certificate/private by alias {}", alias);

			Key key = keyStore.getKey(alias, ProxyContext.keyStorePassword);
			Objects.requireNonNull(key, "Specified key of the KeyStore not found!");

			X509Certificate[] certChain = (X509Certificate[]) keyStore.getCertificateChain(alias);

			builder = SslContextBuilder
				.forServer((PrivateKey) key, certChain)
				.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
				.applicationProtocolConfig(
						new ApplicationProtocolConfig(
								ApplicationProtocolConfig.Protocol.ALPN,
								ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
								ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
								ApplicationProtocolNames.HTTP_2,
								ApplicationProtocolNames.HTTP_1_1)
				);

			sslContextBuilderCache.put(host, builder);
		}

		return builder.build();
	}

	public static void main(String[] args) throws Exception {

	}
}
