package org.jooby.issues;

import org.jooby.FlashScope;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class Issue468 extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.path", ConfigValueFactory.fromAnyRef("/468")));

    use(new FlashScope());

    get("/", req -> req.flash().get("foo"));

    get("/redirect", req -> {
      req.flash("foo", "bar");
      return Results.redirect(req.contextPath() + "/");
    });
  }

  @Test
  public void flashAttributeIsPresentBetweenDiffPaths() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/468/redirect")
        .execute()
        .header("Set-Cookie", "jooby.flash=foo=bar;Version=1;Path=/468;HttpOnly");
  }

  @Test
  public void flashAttributeIsPresentBetweenDiffPathsOnRedirect() throws Exception {
    request()
        .get("/468/redirect")
        .expect("bar");
  }

}
