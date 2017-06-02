package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.jooby.unbescape.XSS;
import org.junit.Test;
import org.unbescape.css.CssStringEscapeLevel;
import org.unbescape.css.CssStringEscapeType;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;
import org.unbescape.javascript.JavaScriptEscapeLevel;
import org.unbescape.javascript.JavaScriptEscapeType;
import org.unbescape.json.JsonEscapeLevel;
import org.unbescape.json.JsonEscapeType;

public class Issue477 extends ServerFeature {

  {
    use(new XSS()
        .css(CssStringEscapeType.BACKSLASH_ESCAPES_DEFAULT_TO_COMPACT_HEXA,
            CssStringEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC)
        .html(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL,
            HtmlEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC)
        .js(JavaScriptEscapeType.SINGLE_ESCAPE_CHARS_DEFAULT_TO_XHEXA_AND_UHEXA,
            JavaScriptEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC)
        .json(JsonEscapeType.SINGLE_ESCAPE_CHARS_DEFAULT_TO_UHEXA,
            JsonEscapeLevel.LEVEL_3_ALL_NON_ALPHANUMERIC));

    get("/477/text", req -> {
      return req.param("text", "html").value();
    });

    get("/477/js", req -> {
      return req.param("text", "js").value();
    });

    get("/477/json", req -> {
      return req.param("text", "json").value();
    });

    get("/477/css", req -> {
      return req.param("text", "css").value();
    });
  }

  @Test
  public void escapeHtml() throws Exception {
    request()
        .get("/477/text?text=%3Ch1%3EX%3C/h1%3E")
        .expect("&lt;h1&gt;X&lt;&sol;h1&gt;");
  }

  @Test
  public void escapeJs() throws Exception {
    request()
        .get("/477/js?text=%3Cscript%3Ealert(%27xss%27)%3C/script%3E")
        .expect("\\x3Cscript\\x3Ealert\\x28\\'xss\\'\\x29\\x3C\\/script\\x3E");
  }

  @Test
  public void escapeJson() throws Exception {
    request()
        .get("/477/json?text=%7B%22x%22:%205%7D")
        .expect("\\u007B\\\"x\\\"\\u003A\\u00205\\u007D");
  }

  @Test
  public void escapeCss() throws Exception {
    request()
        .get("/477/css?text=body%7B%7D")
        .expect("body\\{\\}");
  }

}
