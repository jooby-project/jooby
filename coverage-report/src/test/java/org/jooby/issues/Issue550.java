package org.jooby.issues;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue550 extends ServerFeature {

  public static class Person {
    public String name;
  }

  {
    get("/550", req -> {
      return req.params(Person.class).name;
    });
  }

  @Test
  public void shouldIgnoreEmptyParams() throws Exception {
    request()
        .get("/550?name=pedro&person.sync")
        .expect("pedro")
        .expect(200);
  }

}
