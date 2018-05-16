package org.jooby.assets;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class CsslintTest {

  private static V8EngineFactory engineFactory = new V8EngineFactory();

  @AfterClass
  public static void release() {
    engineFactory.release();
  }

  @Test
  public void name() throws Exception {
    assertEquals("csslint", new Csslint().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals(".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n" +
        "", new Csslint()
        .set(engineFactory)
        .process("/styles.css", ".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n", ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void boxModelErr() throws Exception {
    assertEquals(".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n" +
        "", new Csslint()
        .set(engineFactory)
        .set("box-model", 2).process("/styles.css", ".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n", ConfigFactory.empty()));
  }

}
