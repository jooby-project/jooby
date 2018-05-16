package org.jooby.assets;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;

public class JsHintTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void empty() throws Exception {
    assertEquals("x = 4;",
        new Jshint()
            .set(engineFactory)
            .set("undef", false).process("/x.js", "x = 4;", ConfigFactory.empty()));
  }

  @Test
  public void jqueryPredef() throws Exception {
    assertEquals("$(function () {});",
        new Jshint()
            .set(engineFactory)
            .set("predef", ImmutableMap.of("$", true))
            .process("/x.js", "$(function () {});", ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void undefined() throws Exception {
    new Jshint()
        .set(engineFactory)
        .process("/x.js", "x = 4;", ConfigFactory.empty());
  }

  @Test(expected = AssetException.class)
  public void jquery() throws Exception {
    assertEquals("$(function () {});",
        new Jshint()
            .set(engineFactory)
            .process("/x.js", "$(function () {});", ConfigFactory.empty()));
  }

}
