package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.jooby.MediaType;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class PropsTest {

  @Test
  public void matches() throws Exception {
    assertTrue(new Props().matches(MediaType.css));
    assertTrue(new Props().matches(MediaType.js));
    assertTrue(new Props().matches(MediaType.html));
  }

  @Test
  public void props() throws Exception {
    assertEquals("$.ajax(http://foo.com);",
        new Props().process("/j.s", "$.ajax(${app.url});",
            ConfigFactory
                .empty().withValue("app.url", ConfigValueFactory.fromAnyRef("http://foo.com"))));
  }

  @Test
  public void shouldSupportCustomDelimiters() throws Exception {
    assertEquals("$.ajax(http://foo.com);",
        new Props().set("delims", Arrays.asList("{{", "}}")).process("/j.s", "$.ajax({{app.url}});",
            ConfigFactory
                .empty().withValue("app.url", ConfigValueFactory.fromAnyRef("http://foo.com"))));
  }

}
