package org.jooby.assets;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class JscsTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void valid() throws Exception {
    assertEquals("var a = function (){\n" +
        "\n" +
        "};",
        new Jscs()
            .set(engineFactory )
            .process("/x.js", "var a = function (){\n" +
            "\n" +
            "};", ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void disallowAnonymousFunctions() throws Exception {
    new Jscs().set("disallowAnonymousFunctions", true)
        .set(engineFactory)
        .process("/x.js", "var a = function(){\n" +
        "\n};", ConfigFactory.empty());
  }

  @Test(expected = AssetException.class)
  public void google() throws Exception {
    new Jscs().set("preset", "google")
        .set(engineFactory)
        .process("/x.js", "var a = function(){\n" +
        "\n};", ConfigFactory.empty());
  }

}
