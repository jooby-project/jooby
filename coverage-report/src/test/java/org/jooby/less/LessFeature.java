package org.jooby.less;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.sommeri.less4j.LessCompiler;

public class LessFeature extends ServerFeature {

  {
    use(new Less("/css/**", "/org/jooby/less/{0}"));

    get("/less", req -> {
      LessCompiler compiler = req.require(LessCompiler.class);
      return compiler.compile("p { font-size: 14px; span {color:#000;}}").getCss();
    });
  }

  @Test
  public void handler() throws Exception {
    request()
        .get("/css/less.css")
        .expect(".box {\n" +
            "  color: #fe33ac;\n" +
            "  border-color: #fdcdea;\n" +
            "}\n" +
            ".box div {\n" +
            "  -webkit-box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);\n" +
            "  box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);\n" +
            "}\n" +
            "/*# sourceMappingURL=less.css.map */\n");
  }

  @Test
  public void less() throws Exception {
    request()
        .get("/less")
        .expect("p {\n" +
            "  font-size: 14px;\n" +
            "}\n" +
            "p span {\n" +
            "  color: #000;\n" +
            "}\n");
  }

}
