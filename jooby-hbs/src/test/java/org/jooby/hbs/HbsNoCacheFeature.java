package org.jooby.hbs;

import static org.junit.Assert.assertSame;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.NullTemplateCache;

public class HbsNoCacheFeature extends ServerFeature {

  {
    use(new Hbs());

    get("/", req -> {
      assertSame(NullTemplateCache.INSTANCE, req.require(Handlebars.class).getCache());
      return "noop";
    });
  }

  @Test
  public void hbs() throws Exception {
    request()
        .get("/?model=jooby")
        .expect("noop");
  }

}
