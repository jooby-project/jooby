package org.jooby.jackson;

import java.net.URISyntaxException;

import org.jooby.BodyFormatter;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class JsonAccessFeature extends ServerFeature {

  {
    use(new Jackson());

    get("/formatter", req -> req.require(Key.get(BodyFormatter.class, Names.named("json"))));

    get("/parser", req -> req.require(Key.get(BodyFormatter.class, Names.named("json"))));
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
