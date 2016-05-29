package org.jooby.issues;

import org.jooby.Route;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue384a extends ServerFeature {

  {

    map(Route.Mapper.create("counter", (final Integer v) -> v + 1));
    map(Route.Mapper.create("counter", (final Integer v) -> v + 1));

    get("/counter", () -> 1);
  }

  @Test
  public void shouldIgnoreMapperWithSameName() throws Exception {
    request()
        .get("/counter")
        .expect("2");
  }

}
