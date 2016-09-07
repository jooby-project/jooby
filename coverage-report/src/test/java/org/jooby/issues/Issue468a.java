package org.jooby.issues;

import org.jooby.Cookie;
import org.jooby.FlashScope;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue468a extends ServerFeature {

  {
    use(new FlashScope(new Cookie.Definition("x").httpOnly(false)));

    get("/468", req -> {
      req.flash("foo", "bar");
      return Results.redirect(req.contextPath() + "/");
    });
  }

  @Test
  public void flashAttributeIsPresentBetweenDiffPaths() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/468")
        .execute()
        .header("Set-Cookie", "x=foo=bar;Version=1;Path=/");
  }

}
