package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class YuiJsMinTest {

  @Test
  public void compress() throws Exception {
    assertEquals("function(a){return a*2;}",
        new YuiJs().process("/x.js", "function (longvar) {\nreturn longvar * 2;\n}",
            ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals("function(a){return a*2;}",
        new YuiJs().process("/x.js", "function (longvar) {\nreturn longvar  2;\n}",
            ConfigFactory.empty()));
  }

}
