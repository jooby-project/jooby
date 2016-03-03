package org.jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ArgsConfTest {

  @Test
  public void keypair() {
    Config args = Jooby.args(new String[]{"p.foo=bar", "p.bar=foo" });
    assertEquals("bar", args.getConfig("p").getString("foo"));
    assertEquals("foo", args.getConfig("p").getString("bar"));
  }

  @Test
  public void env() {
    Config args = Jooby.args(new String[]{"foo" });
    assertEquals("foo", args.getConfig("application").getString("env"));
  }

  @Test
  public void defnamespace() {
    Config args = Jooby.args(new String[]{"port=8080" });
    assertEquals(8080, args.getConfig("application").getInt("port"));
    assertEquals(8080, args.getInt("port"));
  }

  @Test
  public void noargs() {
    assertEquals(ConfigFactory.empty(), Jooby.args(null));
    assertEquals(ConfigFactory.empty(), Jooby.args(new String[0]));
  }

}
