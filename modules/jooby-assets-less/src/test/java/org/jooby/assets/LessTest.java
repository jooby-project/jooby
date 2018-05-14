package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;

public class LessTest {

  @Test
  public void basic() throws Exception {
    assertEquals(".class {\n" +
        "  width: 2;\n" +
        "}\n",
        new Less().process("/css/x.js", ".class { width: (1 + 1) }", ConfigFactory.empty()));
  }

  @Test
  public void sourceMap() throws Exception {
    assertEquals(".class {\n" +
        "  width: 2;\n" +
        "}\n" +
        "/*# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi9jc3MveC5qcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTtFQUFTLFFBQUEifQ== */",
        new Less().set("sourceMap", ImmutableMap.of("sourceMapFileInline", true))
            .process("/css/x.js", ".class { width: (1 + 1) }", ConfigFactory.empty()));
  }

  @Test
  public void importDirective() throws Exception {
    assertEquals(".foo {\n" +
        "  background: #900;\n" +
        "}\n" +
        ".class {\n" +
        "  width: 2;\n" +
        "}\n" +
        "",
        new Less().process("/css/x.js", "@import \"foo.less\";\n.class { width: (1 + 1) }",
            ConfigFactory.empty()));
  }

  @Test
  public void importDirectiveCurrentDir() throws Exception {
    assertEquals(".foo {\n" +
        "  background: #900;\n" +
        "}\n" +
        ".class {\n" +
        "  width: 2;\n" +
        "}\n" +
        "",
        new Less().process("/css/x.js", "@import \"bar.less\";\n.class { width: (1 + 1) }",
            ConfigFactory.empty()));
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals("",
        new Less().process("/css/x.js", ".class { width (1 + 1) }", ConfigFactory.empty()));
  }
}
