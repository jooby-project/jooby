package io.jooby.email;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommonsEmailModuleTest {

  @Test
  public void shouldGenerateConfigWithDefaultSetup() {
    String charset = "UTF-8";
    String hostName = "smtp.googlemail.com";
    Config config = ConfigFactory.empty()
        .withValue("application.charset", ConfigValueFactory.fromAnyRef(charset))
        .withValue("mail.hostName", ConfigValueFactory.fromAnyRef(hostName));
    Config result = CommonsEmailModule.mailConfig(config, "mail");

    assertEquals(hostName, result.getString("hostName"));
    assertEquals(charset, result.getString("charset"));
  }

  @Test
  public void shouldGenerateConfigWithCustomSetup() {
    String charset = "UTF-8";
    String username = "u";
    String password = "p";
    String hostName1 = "smtp.googlemail.com";
    String hostName2 = "host2.googlemail.com";
    Config config = ConfigFactory.empty()
        .withValue("application.charset", ConfigValueFactory.fromAnyRef(charset))
        .withValue("otherMail.hostName", ConfigValueFactory.fromAnyRef(hostName2))
        .withValue("mail.hostName", ConfigValueFactory.fromAnyRef(hostName1))
        .withValue("mail.username", ConfigValueFactory.fromAnyRef(username))
        .withValue("mail.password", ConfigValueFactory.fromAnyRef(password));
    Config result = CommonsEmailModule.mailConfig(config, "otherMail");

    assertEquals(hostName2, result.getString("hostName"));
    assertEquals(username, result.getString("username"));
    assertEquals(password, result.getString("password"));
    assertEquals(charset, result.getString("charset"));
  }

  @Test
  public void shouldFailWhenConfigIsMissing() {
    assertThrows(ConfigException.Missing.class,
        () -> CommonsEmailModule.mailConfig(ConfigFactory.empty(), "mail"));
  }
}
