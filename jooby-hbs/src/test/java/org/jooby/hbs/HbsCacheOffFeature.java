package org.jooby.hbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

// TODO: make me a unit test
public class HbsCacheOffFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod"))
        .withValue("hbs.cache", ConfigValueFactory.fromAnyRef("")));

    use(new Hbs());

    get("/", req -> {
      assertSame(NullTemplateCache.INSTANCE, req.getInstance(Handlebars.class).getCache());
      return "noop";
    });
  }

  @Test
  public void hbs() throws Exception {
    assertEquals("noop",
        Request.Get(uri("/").addParameter("model", "jooby").build()).execute()
            .returnContent().asString());
  }

}
