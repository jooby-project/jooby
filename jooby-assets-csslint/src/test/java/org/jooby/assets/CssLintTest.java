package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class CssLintTest {

  @Test
  public void name() throws Exception {
    assertEquals("css-lint", new CssLint().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals(".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n" +
        "", new CssLint().process("/styles.css", ".mybox {\n" +
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
        "", new CssLint().set("box-model", 2).process("/styles.css", ".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n", ConfigFactory.empty()));
  }

}
