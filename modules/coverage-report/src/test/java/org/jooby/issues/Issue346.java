package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.jooby.Request;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class Issue346 extends ServerFeature {

  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface SitemapUrl {
    double priority() default 0.5d;

    String changefreq() default "always";
  }

  @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Role {
    String value();
  }

  @Path("/")
  @SitemapUrl
  public static class Resource {

    @GET
    @SitemapUrl(priority = 1.0)
    @Path("/1")
    public Object sitemap1(final Request req) {
      return req.route().attributes();
    }

    @GET
    @Path("/2")
    public Object sitemap2(final Request req) {
      return req.route().attributes();
    }

    @GET
    @Role("admin")
    @Path("/3")
    public Object admin(final Request req) {
      return req.route().attributes();
    }
  }

  {
    use("/1", (req, rsp) -> {
      assertEquals(ImmutableMap.of("priority", 1.0, "changefreq", "always"),
          req.route().attributes());
    });

    use("/2", (req, rsp) -> {
      assertEquals(ImmutableMap.of("priority", 0.5, "changefreq", "always"),
          req.route().attributes());
    });

    use("/3", (req, rsp) -> {
      assertEquals(ImmutableMap.of("priority", 0.5, "changefreq", "always", "role", "admin"),
          req.route().attributes());
    });
    use(Resource.class);
  }

  @Test
  public void mvcAttrs() throws Exception {
    request().get("/1")
        .expect(value -> {
          Map<String, Object> hash = toMap(value.substring(1, value.length() - 1));
          assertEquals(ImmutableMap.of("priority", "1.0", "changefreq", "always"), hash);
        });

    request().get("/2")
        .expect(value -> {
          Map<String, Object> hash = toMap(value.substring(1, value.length() - 1));
          assertEquals(ImmutableMap.of("priority", "0.5", "changefreq", "always"), hash);
        });

    request().get("/3")
        .expect(value -> {
          Map<String, Object> hash = toMap(value.substring(1, value.length() - 1));
          assertEquals(ImmutableMap.of("priority", "0.5", "changefreq", "always", "role", "admin"),
              hash);
        });
  }

  private Map<String, Object> toMap(final String value) {
    Map<String, Object> hash = new HashMap<>();
    String[] pairs = value.split(",");
    for (String pair : pairs) {
      String[] keyandvalue = pair.trim().split("=");
      hash.put(keyandvalue[0], keyandvalue[1]);
    }
    return hash;
  }

}
