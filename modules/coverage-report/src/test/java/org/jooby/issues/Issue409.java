package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue409 extends ServerFeature {

  public enum Letter {A, B, C}

  {
    get("/409/:letter", req -> req.param("letter").toEnum(Letter.class));
  }

  @Test
  public void caseInsensitveEnumParser() throws Exception {
    request()
        .get("/409/a")
        .expect("A");

    request()
        .get("/409/A")
        .expect("A");
  }

}
