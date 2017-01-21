package org.elasticsearch.plugin.transport.secure;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.transport.netty.NettyTransport;

import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.security.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.indices.breaker.CircuitBreakerService;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * Created by gregsterin on 12/21/16.
 */
public class SecureTransport extends NettyTransport {
    private final String KEYSTORE = "transport.secure.keystore.path";
    private final String KEYSTORE_PASSWORD = "transport.secure.keystore.password";
    private final String KEYSTORE_KEY_PASSWORD = "transport.secure.keystore.keypassword";

    private final String TRUSTSTORE = "transport.secure.truststore.path";
    private final String TRUSTSTORE_PASSWORD = "transport.secure.truststore.password";

    private final String keystorePath;
    private final String keystorePassword;
    private final String keyPassword;
    private final String truststorePath;
    private final String truststorePassword;

    @Inject
    public SecureTransport(Settings settings, ThreadPool threadPool, NetworkService networkService, BigArrays bigArrays, Version version,
                          NamedWriteableRegistry namedWriteableRegistry, CircuitBreakerService circuitBreakerService)
            throws IOException, GeneralSecurityException {
        super(settings, threadPool, networkService, bigArrays, version, namedWriteableRegistry, circuitBreakerService);

        keystorePath = this.settings.get(KEYSTORE);
        keystorePassword = this.settings.get(KEYSTORE_PASSWORD);
        keyPassword = this.settings.get(KEYSTORE_KEY_PASSWORD);
        truststorePath = this.settings.get(TRUSTSTORE);
        truststorePassword = this.settings.get(TRUSTSTORE_PASSWORD);


        logger.info("Secure ES plugin loaded");
    }

    @Override
    public ChannelPipelineFactory configureClientChannelPipelineFactory() {
        return new ClientChannelPipelineFactory(this) {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                logger.debug("Get client pipeline");
                ChannelPipeline channelPipeline = super.getPipeline();
                channelPipeline.addFirst("ssl", setupSslHandler(true));
                return channelPipeline;
            }
        };
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory(String name, Settings settings) {
        return new ServerChannelPipelineFactory(this, name, settings) {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                logger.debug("Get server pipeline");
                ChannelPipeline channelPipeline = super.getPipeline();
                channelPipeline.addFirst("ssl", setupSslHandler(false));
                return channelPipeline;
            }
        };
    }

    private SslHandler setupSslHandler(boolean clientMode)
            throws IOException, GeneralSecurityException {

        SSLContext context = SSLContext.getInstance("TLS");

        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new java.io.FileInputStream(keystorePath), keystorePassword.toCharArray());

        final KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(new java.io.FileInputStream(truststorePath), truststorePassword.toCharArray());

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, keyPassword.toCharArray());

        final TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(clientMode);
        engine.setNeedClientAuth(true);

        return new SslHandler(engine);
    }
}
