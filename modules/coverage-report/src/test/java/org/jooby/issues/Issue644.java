package org.jooby.issues;

import org.jooby.handlers.SSIHandler;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue644 extends ServerFeature {

  {
    get("/i644/*", new SSIHandler());

    get("/644/index.html",
        new SSIHandler("/i644/ssi-jooby-include.html")
            .delimiters("<!-- JOOBY-INCLUDE", "-->"));
  }

  @Test
  public void ssi() throws Exception {
    request()
        .get("/i644/ssi.html")
        .expect("<!-- The template for the note object -->\n" +
            "<script type=\"text/x-template\" id=\"custom-note-template\">\n" +
            "  <ul><li>Item</li></ul>\n" +
            "</script>")
        .header("Content-Type", "text/html;charset=utf-8");

    request()
        .get("/i644/root.html")
        .expect("<!-- The template for the note object -->\n" +
            "<p>l1</p>\n" +
            "<p>l2</p>\n" +
            "")
        .header("Content-Type", "text/html;charset=utf-8");

    request()
        .get("/644/index.html")
        .expect("<!-- The template for the note object -->\n" +
            "<script type=\"text/x-template\" id=\"custom-note-template\">\n" +
            "  <ul><li>Item</li></ul>\n" +
            "</script>")
        .header("Content-Type", "text/html;charset=utf-8");
  }

}
