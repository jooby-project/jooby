package io.jooby;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SslOptionsTest {

  @Test
  public void shouldDoNothingOnMissingPaths() {
    Config config = ConfigFactory.empty().resolve();

    assertEquals(false, SslOptions.from(config).isPresent());
  }

  @Test
  public void shouldFailOnInvalidSslType() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.type", fromAnyRef("xxxx"))
        .resolve();

    assertThrows(UnsupportedOperationException.class, () -> SslOptions.from(config));
  }

  @Test
  public void shouldLoadSelfSigned() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.type", fromAnyRef("self-signed"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.PKCS12, options.getType());
    assertEquals("io/jooby/ssl/localhost.p12", options.getCert());
    assertEquals("changeit", options.getPassword());
  }

  @Test
  public void shouldLoadPKCS12FromConfig() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.type", fromAnyRef("pkcs12"))
        .withValue("ssl.cert", fromAnyRef("ssl/test.p12"))
        .withValue("ssl.password", fromAnyRef("changeit"))
        .withValue("ssl.trust.cert", fromAnyRef("ssl/trust.p12"))
        .withValue("ssl.trust.password", fromAnyRef("pass"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.PKCS12, options.getType());
    assertEquals("ssl/test.p12", options.getCert());
    assertEquals("changeit", options.getPassword());
    assertEquals("ssl/trust.p12", options.getTrustCert());
    assertEquals("pass", options.getTrustPassword());
  }

  @Test
  public void shouldLoadX509FromConfig() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.type", fromAnyRef("x509"))
        .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
        .withValue("ssl.key", fromAnyRef("ssl/test.key"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.X509, options.getType());
    assertEquals("ssl/test.crt", options.getCert());
    assertEquals("ssl/test.key", options.getPrivateKey());
  }

  @Test
  public void shouldLoadX509WithPasswordFromConfig() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.type", fromAnyRef("x509"))
        .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
        .withValue("ssl.key", fromAnyRef("ssl/test.key"))
        .withValue("ssl.password", fromAnyRef("changeit"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.X509, options.getType());
    assertEquals("ssl/test.crt", options.getCert());
    assertEquals("ssl/test.key", options.getPrivateKey());
    assertEquals("changeit", options.getPassword());
  }

  @Test
  public void shouldParseSingleProtocol() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.protocol", fromAnyRef("TLSv1.2"))
        .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
        .withValue("ssl.key", fromAnyRef("ssl/test.key"))
        .withValue("ssl.password", fromAnyRef("changeit"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(Collections.singletonList("TLSv1.2"), options.getProtocol());
  }

  @Test
  public void shouldParseProtocols() {
    Config config = ConfigFactory.empty()
        .withValue("ssl.protocol", fromAnyRef(Arrays.asList("TLSv1.2", "TLSv1.3")))
        .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
        .withValue("ssl.key", fromAnyRef("ssl/test.key"))
        .withValue("ssl.password", fromAnyRef("changeit"))
        .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(Arrays.asList("TLSv1.2", "TLSv1.3"), options.getProtocol());
  }
}
