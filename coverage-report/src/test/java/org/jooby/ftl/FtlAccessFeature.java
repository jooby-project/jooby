package org.jooby.ftl;

import org.jooby.View;
import org.jooby.ftl.Ftl;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class FtlAccessFeature extends ServerFeature {

  {
    use(new Ftl());

    get("/", req -> req.require(Key.get(View.Engine.class, Names.named("ftl"))));
  }

  @Test
  public void access() throws Exception {
    request()
      .get("/")
      .expect("ftl");
  }

}
