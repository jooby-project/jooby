package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Issue354 extends ServerFeature {

  {
    get("/:glob", req -> req.route().reverse(req.route().glob() ? "yes" : "no"));
    get("/g/:glob",
        req -> req.route().reverse(ImmutableMap.of("glob", req.route().glob() ? "yes" : "no")));
    get("/s/*", req -> req.route().glob() ? "yes" : "no");
    get("/q/t?st", req -> req.route().glob() ? "yes" : "no");
    get("/w/**/*", req -> req.route().glob() ? "yes" : "no");
  }

  @Test
  public void glob() throws Exception {
    request().get("/glob")
        .expect("/no");
    request().get("/g/glob")
        .expect("/g/no");

    request().get("/s/glob")
        .expect("yes");
    request().get("/q/test")
        .expect("yes");
    request().get("/w/test")
        .expect("yes");
  }

}
