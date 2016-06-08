package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jooby.FlashScope;
import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue397 extends ServerFeature {

  {
    get("/397/noflash", req -> req.flash());

    use(new FlashScope());

    get("/397/flash", req -> req.flash());

    get("/397/discard", req -> req.flash().remove("success"));

    post("/397/reset", req -> {
      req.flash("foo", "bar");
      return Results.redirect("/397");
    });

    post("/397/flash", req -> {
      req.flash("success", "Thank you!");
      return Results.redirect("/397");
    });

    get("/397/untouch", req -> "untouch");

    err((req, rsp, err) -> {
      rsp.send(err.getMessage());
    });
  }

  @Test
  public void noFlashScope() throws Exception {
    request()
        .get("/397/noflash")
        .expect("Bad Request(400): Flash scope isn't available. Install via: use(new FlashScope());");
  }

  @Test
  public void shouldCreateAndDestroyFlashCookie() throws Exception {
    request()
        .post("/397/flash")
        .expect(302)
        .header("Set-Cookie", setCookie -> {
          assertEquals("flash=success=Thank+you%21;Version=1", setCookie);
          request()
              .get("/397/flash")
              .header("Cookie", setCookie)
              .expect("{success=Thank you!}")
              .header("Set-Cookie", clearCookie -> {
                assertTrue(clearCookie.startsWith("flash=;Version=1;Max-Age=0;"));
              });
        });
  }

  @Test
  public void shouldNotCreateCookieWhenFlashStateDontChange() throws Exception {
    request()
        .get("/397/untouch")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertEquals(null, setCookie);
        });
  }

  @Test
  public void shouldRecreateCookieOnReset() throws Exception {
    request()
        .post("/397/flash")
        .expect(302)
        .header("Set-Cookie", setCookie1 -> {
          assertEquals("flash=success=Thank+you%21;Version=1", setCookie1);
          request()
              .post("/397/reset")
              .header("Cookie", setCookie1)
              .expect(302)
              .header("Set-Cookie", setCookie2 -> {
                assertEquals("flash=success=Thank+you%21&foo=bar;Version=1", setCookie2);
                request()
                    .get("/397/flash")
                    .header("Cookie", setCookie2)
                    .expect("{success=Thank you!, foo=bar}")
                    .header("Set-Cookie", clearCookie -> {
                      assertTrue(clearCookie.startsWith("flash=;Version=1;Max-Age=0;"));
                    });
              });
        });
  }

  @Test
  public void shouldClearFlashCookieWhenEmpty() throws Exception {
    request()
        .get("/397/discard")
        .header("Cookie", "flash=success=OK;Version=1")
        .expect(200)
        .header("Set-Cookie", setCookie -> {
          assertTrue(setCookie.startsWith("flash=;Version=1;Max-Age=0;"));
        });
  }
}
