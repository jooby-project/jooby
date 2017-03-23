package org.jooby.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

  @Test
  public void ignoreMissingProps() throws Exception {
    assertEquals("$.ajax(${app.url});",
        new Props()
            .set("ignoreMissing", true)
            .process("/j.s", "$.ajax(${app.url});", ConfigFactory.empty()));
  }

  @Test
  public void missingProp() throws Exception {
    try {
      new Props().process("/j.s", "$.ajax(${cpath}/service);", ConfigFactory.empty());
      fail();
    } catch (AssetException ex) {
      assertEquals("\t/j.s:1:8: Missing ${cpath} at 1:8",
          ex.getMessage());
      assertEquals("Missing ${cpath} at 1:8", ex.get());
    }

    try {
      new Props().process("/j.s", "$.ajax(\n\n   ${cpath}/service);", ConfigFactory.empty());
      fail();
    } catch (AssetException ex) {
      assertEquals("\t/j.s:3:4: Missing ${cpath} at 3:4",
          ex.getMessage());
      assertEquals("Missing ${cpath} at 3:4", ex.get());
    }
  }

  @Test
  public void defaultErr() throws Exception {
    try {
      new Props().process("/j.s", null, ConfigFactory.empty());
      fail();
    } catch (AssetException ex) {
      assertEquals("\t/j.s:-1:-1: Text is required.", ex.getMessage());
    }

  }

}
