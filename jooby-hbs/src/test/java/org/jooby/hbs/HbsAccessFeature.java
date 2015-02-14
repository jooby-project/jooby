package org.jooby.hbs;

import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class HbsAccessFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> req.require(Key.get(View.Engine.class, Names.named("hbs"))));
  }

  @Test
  public void access() throws Exception {
    request()
      .get("/")
      .expect("hbs");
  }

}
