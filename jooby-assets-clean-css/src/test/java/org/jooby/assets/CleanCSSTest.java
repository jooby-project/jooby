package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.ConfigFactory;

public class CleanCSSTest {

  @Test
  public void name() throws Exception {
    assertEquals("clean-css", new CleanCss().name());
  }

  @Test
  public void defaults() throws Exception {
    assertEquals("a{font-weight:700}",
        new CleanCss().process("/styles.css", "a {\n  font-weight:bold;\n}\n",
            ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals("a{font-weight:700}",
        new CleanCss().process("/styles.css", "a {\n  font-weight:bold;\n\n",
            ConfigFactory.empty()));
  }

  @Test
  public void importDirective() throws Exception {
    assertEquals(".foo{color:#fff}a{font-weight:700}",
        new CleanCss().process("/styles.css", "@import 'foo.css';\na {\n  font-weight:bold;\n}\n",
            ConfigFactory.empty()));
  }

}
