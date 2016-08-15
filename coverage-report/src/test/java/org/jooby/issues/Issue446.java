package org.jooby.issues;

import java.net.URISyntaxException;

import org.jooby.Results;
import org.jooby.jade.Jade;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue446 extends ServerFeature {

  {
    use(new Jade(".jade"));

    get("/446", () -> Results.html("446"));
  }

  @Test
  public void shouldNotGetABlankPage() throws URISyntaxException, Exception {
    request()
        .get("/446")
        .expect(404);
  }
}
