package org.jooby.hbs;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class HbsCustomErrFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/err", () -> {
      throw new IllegalArgumentException("intentional err");
    });
  }

  @Test
  public void err() throws Exception {
    request().get("/err").expect(400).startsWith("<!doctype html>\n" +
        "<html>\n" +
        "<head>\n" +
        "<meta charset=\"UTF-8\">\n" +
        "<style>\n" +
        "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n" +
        "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n" +
        "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n" +
        "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n" +
        "hr {background-color: #f7f7f9;}\n" +
        "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n" +
        "p {padding-left: 20px;}\n" +
        "p.tab {padding-left: 40px;}\n" +
        "</style>\n" +
        "<title>\n" +
        "400 Bad Request\n" +
        "</title>\n" +
        "<body>\n" +
        "<h1>Bad Request</h1>\n" +
        "<hr><h2>message: intentional err</h2>\n" +
        "<h2>status: 400</h2>");
  }
}
