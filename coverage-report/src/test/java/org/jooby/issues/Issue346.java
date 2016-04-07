package org.jooby.issues;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.Request;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

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
      assertEquals("{priority=1.0, changefreq=always}", req.route().attributes().toString());
    });

    use("/2", (req, rsp) -> {
      assertEquals("{priority=0.5, changefreq=always}", req.route().attributes().toString());
    });

    use("/3", (req, rsp) -> {
      assertEquals("{role=admin, priority=0.5, changefreq=always}",
          req.route().attributes().toString());
    });
    use(Resource.class);
  }

  @Test
  public void mvcAttrs() throws Exception {
    request().get("/1")
        .expect("{priority=1.0, changefreq=always}");

    request().get("/2")
        .expect("{priority=0.5, changefreq=always}");

    request().get("/3")
        .expect("{role=admin, priority=0.5, changefreq=always}");
  }

}
