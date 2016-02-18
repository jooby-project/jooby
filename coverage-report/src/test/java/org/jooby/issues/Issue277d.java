package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue277d extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("dev")));
  }

  @Test
  public void shouldDisplayStacktrace() throws Exception {
    request().get("/intentional-err")
        .expect(s -> s.startsWith("<!doctype html>\n" +
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
            "404 Not Found\n" +
            "</title>\n" +
            "<body>\n" +
            "<h1>Not Found</h1>\n" +
            "<hr><h2>message: Not Found(404): /intentional-err</h2>\n" +
            "<h2>status: 404</h2>\n" +
            "<h2>stack:</h2>\n"));
  }

}
