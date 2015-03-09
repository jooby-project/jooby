package org.jooby.jackson;

import java.net.URISyntaxException;

import org.jooby.Body;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class JsonAccessFeature extends ServerFeature {

  {
    use(new Json());

    get("/formatter", req -> req.require(Key.get(Body.Formatter.class, Names.named("json"))));

    get("/parser", req -> req.require(Key.get(Body.Formatter.class, Names.named("json"))));
  }

  @Test
  public void formatter() throws URISyntaxException, Exception {
    request()
        .get("/formatter")
        .expect("json");
  }

  @Test
  public void parser() throws URISyntaxException, Exception {
    request()
        .get("/parser")
        .expect("json");
  }

}
