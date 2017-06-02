package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class JscsTest {

  @Test
  public void valid() throws Exception {
    assertEquals("var a = function (){\n" +
        "\n" +
        "};",
        new Jscs().process("/x.js", "var a = function (){\n" +
            "\n" +
            "};", ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void disallowAnonymousFunctions() throws Exception {
    new Jscs().set("disallowAnonymousFunctions", true).process("/x.js", "var a = function(){\n" +
        "\n};", ConfigFactory.empty());
  }

  @Test(expected = AssetException.class)
  public void google() throws Exception {
    new Jscs().set("preset", "google").process("/x.js", "var a = function(){\n" +
        "\n};", ConfigFactory.empty());
  }

}
