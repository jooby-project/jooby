package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue384b extends ServerFeature {

  {

    mapper(((final Integer value) -> value * 2));

    mapper((v -> Integer.parseInt(v.toString())));

    get("/4", () -> {
      return "2";
    });
  }

  @Test
  public void shouldStackOrChainMapper() throws Exception {
    request()
        .get("/4")
        .expect("4");
  }

}
