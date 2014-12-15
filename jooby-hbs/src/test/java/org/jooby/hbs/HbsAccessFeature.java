package org.jooby.hbs;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.jooby.View;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class HbsAccessFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> req.getInstance(Key.get(View.Engine.class, Names.named("hbs"))));
  }

  @Test
  public void access() throws Exception {
    assertEquals("hbs", Request.Get(uri("/").build()).execute().returnContent().asString());
  }

}
