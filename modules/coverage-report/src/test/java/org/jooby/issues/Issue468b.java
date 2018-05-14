package org.jooby.issues;

import org.jooby.FlashScope;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue468b extends ServerFeature {

  {
    use(new FlashScope());

    get("/468", req -> req.flash().get("foo"));

    get("/468/redirect", req -> {
      req.flash("foo", "bar");
      return Results.redirect("/468");
    });
  }

  @Test
  public void flashAttributeIsPresentBetweenDiffPaths() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/468/redirect")
        .execute()
        .header("Set-Cookie", "jooby.flash=foo=bar;Version=1;Path=/;HttpOnly");
  }

  @Test
  public void flashAttributeIsPresentBetweenDiffPathsOnRedirect() throws Exception {
    request()
        .get("/468/redirect")
        .expect("bar");
  }

}
