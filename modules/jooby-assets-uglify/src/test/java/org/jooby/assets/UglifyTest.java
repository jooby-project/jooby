package org.jooby.assets;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;

public class UglifyTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void removeSpaces() throws Exception {
    assertEquals("var x=1;",
        new Uglify()
            .set(engineFactory)
            .process("/test.js", "\nvar    x   =  1;", ConfigFactory.empty()));
  }

  @Test
  public void mangle() throws Exception {
    String statement = "function x(longvarname) {\nconsole.log(longvarname);\n};";
    String rsp = new Uglify().set(engineFactory).process("/test.js", statement, ConfigFactory.empty());
    assertTrue(rsp.startsWith("function x("));
    assertTrue(!rsp.startsWith("function x(longvarname)"));
    assertTrue(rsp.length() < statement.length());
  }

  @Test
  public void nomangle() throws Exception {
    String statement = "function x(longvarname){console.log(longvarname)}";
    String rsp = new Uglify().set(engineFactory).set("mangle", null).process("/test.js", statement,
        ConfigFactory.empty());
    assertEquals(statement, rsp);
  }

  @Test
  public void beautify() throws Exception {
    String statement = "function x(longvarname){console.log(longvarname)}";
    String rsp = new Uglify().set(engineFactory).set("mangle", null).set("output", ImmutableMap.of("beautify", true))
        .process("/test.js", statement, ConfigFactory.empty());
    assertEquals("function x(longvarname) {\n" +
        "    console.log(longvarname);\n" +
        "}", rsp);
  }

}
