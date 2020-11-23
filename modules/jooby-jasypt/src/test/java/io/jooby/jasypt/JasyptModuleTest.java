package io.jooby.jasypt;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Jooby;

public class JasyptModuleTest {

  @Test
  public void shouldDecryptProperties() {
    Config config = ConfigFactory.empty()
        .withValue("jasypt.password", fromAnyRef("password"))
        .withValue("enc.property", fromAnyRef("uTSqb9grs1+vUv3iN8lItC0kl65lMG+8"));
    Jooby app = new Jooby() {{
      getEnvironment().setConfig(config);

      install(new JasyptModule());
    }};

    Config newConfig = app.getConfig();
    assertNotEquals(config, newConfig);

    assertEquals("Password@1", newConfig.getString("property"));
    assertEquals("uTSqb9grs1+vUv3iN8lItC0kl65lMG+8", newConfig.getString("enc.property"));
    assertNotNull(app.require(PBEStringEncryptor.class));
    assertNotNull(app.require(StandardPBEStringEncryptor.class));
  }

  @Test
  public void shouldIgnoreWhenNothingToDecrypt() {
    Config config = ConfigFactory.empty()
        .withValue("jasypt.password", fromAnyRef("password"))
        .withValue("foo.property", fromAnyRef("uTSqb9grs1+vUv3iN8lItC0kl65lMG+8"));
    Jooby app = new Jooby() {{
      getEnvironment().setConfig(config);

      install(new JasyptModule());
    }};

    Config newConfig = app.getConfig();
    assertEquals(config, newConfig);

    assertEquals(false, newConfig.hasPath("property"));
    assertNotNull(app.require(PBEStringEncryptor.class));
    assertNotNull(app.require(StandardPBEStringEncryptor.class));
  }

  @Test
  public void shouldCreatePooledEncryptor() {
    Config config = ConfigFactory.empty()
        .withValue("jasypt.password", fromAnyRef("password"))
        .withValue("jasypt.poolSize", fromAnyRef(2))
        .withValue("enc.property", fromAnyRef("uTSqb9grs1+vUv3iN8lItC0kl65lMG+8"));
    Jooby app = new Jooby() {{
      getEnvironment().setConfig(config);

      install(new JasyptModule());
    }};

    assertNotNull(app.require(PBEStringEncryptor.class));
    assertNotNull(app.require(PooledPBEStringEncryptor.class));
  }
}
