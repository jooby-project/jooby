package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.jooby.spi.HttpHandler;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettySslContext.class, SslContextBuilder.class, OpenSsl.class,
    ApplicationProtocolConfig.class })
public class NettySslContextTest {

  Config conf = ConfigFactory.empty()
      .withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(false))
      .withValue("application.tmpdir", ConfigValueFactory.fromAnyRef("target"))
      .withValue("application.securePort", ConfigValueFactory.fromAnyRef(8443))
      .withValue("ssl.keystore.cert", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"))
      .withValue("ssl.keystore.key", ConfigValueFactory.fromAnyRef("org/jooby/unsecure.key"));

  @Test
  public void sslContext() throws Exception {
    new NettySslContext();
    new MockUnit(HttpHandler.class)
        .expect(ssl(null))
        .run(unit -> {
          assertNotNull(NettySslContext.build(conf));
        });
  }

  @Test
  public void sslTrustCert() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(ssl(null))
        .expect(unit-> {
          SslContextBuilder scb = unit.get(SslContextBuilder.class);
          expect(scb.trustManager(Paths.get("target", "unsecure.crt").toFile())).andReturn(scb);
          expect(scb.clientAuth(ClientAuth.REQUIRE)).andReturn(scb);
        })
        .run(unit -> {
          assertNotNull(NettySslContext.build(conf.withValue("ssl.trust.cert",
              ConfigValueFactory.fromAnyRef("org/jooby/unsecure.crt"))));
        });
  }

  @Test
  public void sslContextWithPassword() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(ssl("password"))
        .run(unit -> {
          assertNotNull(NettySslContext
              .build(conf.withValue("ssl.keystore.password",
                  ConfigValueFactory.fromAnyRef("password"))));
        });
  }

  @Test
  public void http2jdk() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(ssl(null))
        .expect(unit -> {
          unit.mockStatic(OpenSsl.class);

          expect(OpenSsl.isAlpnSupported()).andReturn(false);
        })
        .expect(alpn(SslProvider.JDK))
        .run(unit -> {
          assertNotNull(NettySslContext
              .build(conf.withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(true))));
        });
  }

  private Block ssl(final String password) {
    return unit -> {
      unit.mockStatic(SslContextBuilder.class);

      SslContext sslCtx = unit.mock(SslContext.class);

      SslContextBuilder scb = unit.mock(SslContextBuilder.class);
      expect(scb.build()).andReturn(sslCtx);

      expect(SslContextBuilder.forServer(Paths.get("target", "unsecure.crt").toFile(),
          Paths.get("target", "unsecure.key").toFile(), password)).andReturn(scb);

      unit.registerMock(SslContextBuilder.class, scb);
    };
  }

  @Test
  public void http2OpenSSL() throws Exception {
    new MockUnit(HttpHandler.class)
        .expect(ssl(null))
        .expect(unit -> {
          unit.mockStatic(OpenSsl.class);

          expect(OpenSsl.isAlpnSupported()).andReturn(true);
        })
        .expect(alpn(SslProvider.OPENSSL))
        .run(unit -> {
          assertNotNull(NettySslContext
              .build(conf.withValue("server.http2.enabled", ConfigValueFactory.fromAnyRef(true))));
        });
  }

  private Block alpn(final SslProvider provider) {
    return unit -> {
      SslContextBuilder scb = unit.get(SslContextBuilder.class);
      expect(scb.sslProvider(provider)).andReturn(scb);
      expect(scb.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE))
          .andReturn(scb);

      ApplicationProtocolConfig apc = unit.constructor(ApplicationProtocolConfig.class)
          .args(Protocol.class, SelectorFailureBehavior.class,
              SelectedListenerFailureBehavior.class, List.class)
          .build(Protocol.ALPN,
              SelectorFailureBehavior.NO_ADVERTISE,
              SelectedListenerFailureBehavior.ACCEPT,
              Arrays.asList(ApplicationProtocolNames.HTTP_2,
                  ApplicationProtocolNames.HTTP_1_1));
      expect(scb.applicationProtocolConfig(apc)).andReturn(scb);
    };
  }

  @Test
  public void shouldNotCopyFileIfPresent() throws Exception {
    assertNotNull(NettySslContext.toFile(Paths.get("pom.xml").toString(), "target"));
  }

  @Test
  public void shouldCopyCpFile() throws Exception {
    assertNotNull(NettySslContext.toFile(Paths.get("org/jooby/unsecure.crt").toString(), "target"));
  }

  @Test(expected = FileNotFoundException.class)
  public void shouldFailOnMissingFile() throws Exception {
    assertNotNull(NettySslContext.toFile(Paths.get("org/jooby/missing.crt").toString(), "target"));
  }
}
