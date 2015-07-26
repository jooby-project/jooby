package org.jooby.hbs;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import org.jooby.Results;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.github.jknack.handlebars.ValueResolver;

public class HbsCustomValueResolverFeature extends ServerFeature {

  public static class VR implements ValueResolver {

    @Override
    public Object resolve(final Object context, final String name) {
      return "VR";
    }

    @Override
    public Object resolve(final Object context) {
      return "VR";
    }

    @Override
    public Set<Entry<String, Object>> propertySet(final Object context) {
      return Collections.emptySet();
    }
  }

  {
    use(new Hbs().with(new VR()));

    get("/", req -> Results.html("org/jooby/hbs/index"));
  }

  @Test
  public void shouldInjectHelpers() throws Exception {
    request()
        .get("/")
        .expect("<html><title>VR:VR</title><body>VR</body></html>");
  }

}
