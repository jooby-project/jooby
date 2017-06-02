package org.jooby.issues;

import java.util.Map;
import java.util.stream.Collectors;

import org.jooby.Mutant;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue514 extends ServerFeature {

  {
    get("/514", req -> {
      Map<String, Mutant> params = req.params().toMap();
      return req.path() + "?" + params.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue().value()).collect(Collectors.joining("&"));
    });
  }

  @Test
  public void shouldReCreateQueryString() throws Exception {
    request()
        .get("/514?foo=1&bar=2&baz=3")
        .expect(v -> {
          System.out.println(v);
        });
  }

}
