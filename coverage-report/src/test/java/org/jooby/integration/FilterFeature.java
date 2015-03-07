package org.jooby.integration;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class FilterFeature extends ServerFeature {

  {

    use("*", (req, rsp, chain) -> {
      chain.next(req, rsp);
    });

    get("/no-next", (req, rsp, chain) -> {
    });

    get("/no-next", (req, rsp, chain) -> {
      throw new IllegalStateException("Should NOT execute ever");
    });

    get("/before", (req, rsp, chain) -> {
      rsp.header("before", "before");
      chain.next(req, rsp);
    });

    get("/before", (req, rsp) -> {
      rsp.send(rsp.header("before").value());
    });

    get("/after", (req, rsp, chain) -> {
      chain.next(req, rsp);
      rsp.header("after", "after");
    });

    get("/after", (req, rsp) -> {
      rsp.send(rsp.header("after").toOptional(String.class).orElse("after-missing"));
    });

    get("/commit", (req, rsp, chain) -> {
      rsp.send("commit");
    });

    get("/commitx2", (req, rsp, chain) -> {
      rsp.send("commit1");
      chain.next(req, rsp);
    });

    get("/commitx2", (req, rsp) -> {
      rsp.send("ignored");
    });

    get("/redirect", (req, rsp, chain) -> {
      rsp.redirect("/commit");
      chain.next(req, rsp);
    });

    get("/redirect", (req, rsp) -> {
      rsp.send("ignored");
    });

  }

  @Test
  public void nextFilterShouldNeverBeExecutedWhenChainNextIsMissing() throws Exception {
    request()
        .get("/no-next")
        .expect(200)
        .empty();
  }

  @Test
  public void globalFilterShouldNOTAffect404Response() throws Exception {
    request()
        .get("/404")
        .expect(404);
  }

  @Test
  public void beforeFilterShouldBeExecuted() throws Exception {
    request()
        .get("/before")
        .expect(200)
        .expect("before")
        .header("before", "before");
  }

  @Test
  public void headerAfterResponseCommittedAreIgnored() throws Exception {
    request()
        .get("/after")
        .expect(200)
        .expect("after-missing")
        .header("after", (String) null);
  }

  @Test
  public void commitIsPossibleFromFilter() throws Exception {
    request()
        .get("/commit")
        .expect(200)
        .expect("commit");

  }

  @Test
  public void redirectIsPossibleFromFilter() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/redirect")
        .expect(302)
        .empty();

  }

  @Test
  public void secondCommitIsIgnored() throws Exception {
    request()
        .get("/commitx2")
        .expect(200)
        .expect("commit1");
  }

}
