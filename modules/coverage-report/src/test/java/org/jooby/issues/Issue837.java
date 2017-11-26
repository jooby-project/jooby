package org.jooby.issues;

import org.jooby.FlashScope;
import org.jooby.Request;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class Issue837 extends ServerFeature {

  {
    use(new FlashScope());

    get("/837", req -> {
      Request.Flash flash = req.flash();
      flash.put("foo", "bar");
      return Results.redirect("/837/1");
    });

    get("/837/1", req -> {
      Request.Flash flash = req.flash();
      flash.keep();
      return Results.redirect("/837/" + flash.get("foo"));
    });

    get("/837/bar", req -> {
      Request.Flash flash = req.flash();
      return flash.get("foo") + flash.size();
    });

    get("/837/empty", req -> {
      Request.Flash flash = req.flash();
      flash.put("foo", "bar");
      return Results.redirect("/837/2");
    });

    get("/837/2", req -> {
      Request.Flash flash = req.flash();
      flash.keep();
      flash.clear();
      return "clear";
    });

    err((req, rsp, err) -> {
      rsp.send(err.getMessage());
    });
  }

  @Test
  public void shouldKeepFlash() throws Exception {
    request()
        .get("/837")
        .expect("bar1")
        .header("Set-Cookie", clearCookie -> {
          assertTrue(clearCookie.startsWith("jooby.flash=;Version=1;Path=/;HttpOnly;Max-Age=0;"));
        });
  }

  @Test
  public void shouldClearCookieOnEmptyKeepFlash() throws Exception {
    request()
        .get("/837/empty")
        .expect("clear")
        .header("Set-Cookie", clearCookie -> {
          assertTrue(clearCookie.startsWith("jooby.flash=;Version=1;Path=/;HttpOnly;Max-Age=0;"));
        });
  }
}
