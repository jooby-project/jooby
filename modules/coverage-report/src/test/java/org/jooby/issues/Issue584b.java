package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Issue584b extends ServerFeature {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  {

    err((req, rsp, err) -> {
      log.error("{}", req.path(), err);
    });
  }

  @Test
  public void silentFaviconRequest() throws Exception {
    request()
        .get("/favicon.ico")
        .expect(404)
        .expect("");
  }
}
