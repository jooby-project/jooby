package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class YuiCssMinTest {

  @Test
  public void compress() throws Exception {
    assertEquals(".foo{color:black}", new YuiCss().process("/x.css",
        "/*CSS*/\n.foo {\n  color:black;\n}\n", ConfigFactory.empty()));
  }
}
