package org.jooby.jackson;

import java.net.URISyntaxException;

import org.jooby.Renderer;
import org.jooby.json.Jackson;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class JsonAccessFeature extends ServerFeature {

  {
    use(new Jackson());

    get("/formatter", req -> req.require(Key.get(Renderer.class, Names.named("json"))));

    get("/parser", req -> req.require(Key.get(Renderer.class, Names.named("json"))));

    get("/err", () -> {throw new IllegalArgumentException("intentional err");});
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

  @Test
  public void err() throws URISyntaxException, Exception {
    request()
        .get("/err")
        .header("Accept", "application/json")
        .expect(400)
        .header("Content-Type", "application/json;charset=utf-8")
        .startsWith("{\"message\":\"intentional err\",\"stacktrace\":[");
  }

}
