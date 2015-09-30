package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Less4jTest {

  static Config conf = ConfigFactory.empty()
      .withValue("application.charset", ConfigValueFactory.fromAnyRef("UTF-8"));

  @Test
  public void basic() throws Exception {
    assertEquals(".class {\n" +
        "  width: 2;\n" +
        "}\n",
        new Less()
            .process("/css/x.js", ".class { width: (1 + 1) }", conf));
  }

  @Test
  public void sourceMap() throws Exception {
    assertEquals(".class {\n" +
        "  width: 2;\n" +
        "}\n" +
        "/*# sourceMappingURL=data:application/json;base64,ewoidmVyc2lvbiI6MywKImZpbGUiOiIvY3NzL3guY3NzIiwKImxpbmVDb3VudCI6MSwKIm1hcHBpbmdzIjoiQUFBQUE7IiwKInNvdXJjZXMiOlsiL2Nzcy94LmpzIl0sCiJzb3VyY2VzQ29udGVudCI6WyIuY2xhc3MgeyB3aWR0aDogKDEgKyAxKSB9Il0sCiJuYW1lcyI6WyIuY2xhc3MiXQp9Cg== */\n"
        +
        "",
        new Less()
            .set("sourceMap.linkSourceMap", true)
            .process("/css/x.js", ".class { width: (1 + 1) }", conf));
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
        new Less()
            .process("/css/x.js", "@import \"foo.less\";\n.class { width: (1 + 1) }", conf));
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
        new Less()
            .process("/css/x.js", "@import \"bar.less\";\n.class { width: (1 + 1) }", conf));
  }

  @Test(expected = AssetException.class)
  public void error() throws Exception {
    assertEquals("", new Less().process("/css/x.js", ".class { width (1 + 1) }", conf));
  }
}
