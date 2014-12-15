package org.jooby.hbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.fluent.Request;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

//TODO: make me a unit test
public class HbsWithCacheFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("application.env", ConfigValueFactory.fromAnyRef("prod")));

    use(new Hbs());

    get("/", req -> {
      assertTrue(req.getInstance(Handlebars.class).getCache() instanceof GuavaTemplateCache);
      return "guava";
    });
  }

  @Test
  public void hbs() throws Exception {
    assertEquals("guava",
        Request.Get(uri("/").addParameter("model", "jooby").build()).execute()
            .returnContent().asString());
  }

}
