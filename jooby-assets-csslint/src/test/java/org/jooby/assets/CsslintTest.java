package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class CsslintTest {

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
        "", new Csslint().process("/styles.css", ".mybox {\n" +
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
        "", new Csslint().set("box-model", 2).process("/styles.css", ".mybox {\n" +
        "    border: 1px solid black;\n" +
        "    padding: 5px;\n" +
        "    width: 100px;\n" +
        "}\n", ConfigFactory.empty()));
  }

}
