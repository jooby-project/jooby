package org.jooby.netty.issues;

import java.net.URI;
import java.net.URISyntaxException;

import org.jooby.Jooby;
import org.jooby.internal.RoutePattern;
import org.jooby.test.Client;
import org.jooby.test.JoobyRule;
import org.junit.ClassRule;
import org.junit.Test;

public class Issue900 {
  public static class ArticleApp extends Jooby {
    static final RoutePattern USER_TITLE = new RoutePattern("GET", "/:user/:title");

    {
      get(USER_TITLE.pattern(), req -> {
        return req.param("title").value() + " written by " + req.param("user").value();
      });
    }

    public static String urlUserTitle(String user, String title) {
      return USER_TITLE.reverse(user, title);
    }
  }

  @ClassRule
  public static JoobyRule app = new JoobyRule(new ArticleApp());

  @ClassRule
  public static Client client = new Client();

  @Test
  public void clean() throws Exception {
    client.get(ArticleApp.urlUserTitle("shakespeare", "othello"))
      .execute()
      .expect("othello written by shakespeare");
  }

  @Test
  public void slash() throws Exception {
    client.get(ArticleApp.urlUserTitle("not/shakespeare", "othello"))
      .execute()
      .expect("othello written by not/shakespeare");
  }

  @Test
  public void encodedSlash() throws Exception {
    client.get(ArticleApp.urlUserTitle("not%2Fshakespeare", "othello"))
      .execute()
      .expect("othello written by not%2Fshakespeare");
  }
}
