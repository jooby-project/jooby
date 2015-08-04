package org.jooby;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.function.Consumer;

import org.jooby.handlers.Cors;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CorsTest {

  @Test
  public void defaults() {
    cors(cors -> {
      assertEquals(true, cors.anyOrigin());
      assertEquals(true, cors.enabled());
      assertEquals(Arrays.asList("*"), cors.origin());
      assertEquals(true, cors.credentials());

      assertEquals(true, cors.allowMethod("get"));
      assertEquals(true, cors.allowMethod("post"));
      assertEquals(Arrays.asList("GET", "POST"), cors.allowedMethods());

      assertEquals(true, cors.allowHeader("X-Requested-With"));
      assertEquals(true, cors.allowHeader("Content-Type"));
      assertEquals(true, cors.allowHeader("Accept"));
      assertEquals(true, cors.allowHeader("Origin"));
      assertEquals(true, cors.allowHeaders("X-Requested-With", "Content-Type", "Accept", "Origin"));
      assertEquals(Arrays.asList("X-Requested-With", "Content-Type", "Accept", "Origin"),
          cors.allowedHeaders());

      assertEquals(1800, cors.maxAge());

      assertEquals(Arrays.asList(), cors.exposedHeaders());

      assertEquals(false, cors.withoutCreds().credentials());

      assertEquals(false, cors.disabled().enabled());
    });
  }

  @Test
  public void origin() {
    cors(baseconf().withValue("origin", fromAnyRef("*")), cors -> {
      assertEquals(true, cors.anyOrigin());
      assertEquals(true, cors.allowOrigin("http://foo.com"));
    });

    cors(baseconf().withValue("origin", fromAnyRef("http://*.com")), cors -> {
      assertEquals(false, cors.anyOrigin());
      assertEquals(true, cors.allowOrigin("http://foo.com"));
      assertEquals(true, cors.allowOrigin("http://bar.com"));
    });

    cors(baseconf().withValue("origin", fromAnyRef("http://foo.com")), cors -> {
      assertEquals(false, cors.anyOrigin());
      assertEquals(true, cors.allowOrigin("http://foo.com"));
      assertEquals(false, cors.allowOrigin("http://bar.com"));
    });
  }

  @Test
  public void allowedMethods() {
    cors(baseconf().withValue("allowedMethods", fromAnyRef("GET")), cors -> {
      assertEquals(true, cors.allowMethod("GET"));
      assertEquals(true, cors.allowMethod("get"));
      assertEquals(false, cors.allowMethod("POST"));
    });

    cors(baseconf().withValue("allowedMethods", fromAnyRef(asList("get", "post"))), cors -> {
      assertEquals(true, cors.allowMethod("GET"));
      assertEquals(true, cors.allowMethod("get"));
      assertEquals(true, cors.allowMethod("POST"));
    });
  }

  @Test
  public void requestHeaders() {
    cors(baseconf().withValue("allowedHeaders", fromAnyRef("*")), cors -> {
      assertEquals(true, cors.anyHeader());
      assertEquals(true, cors.allowHeader("Custom-Header"));
    });

    cors(baseconf().withValue("allowedHeaders", fromAnyRef(asList("X-Requested-With", "*"))),
        cors -> {
          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.anyHeader());
        });

    cors(
        baseconf().withValue("allowedHeaders",
            fromAnyRef(asList("X-Requested-With", "Content-Type", "Accept", "Origin"))),
        cors -> {
          assertEquals(false, cors.anyHeader());
          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.allowHeader("Content-Type"));
          assertEquals(true, cors.allowHeader("Accept"));
          assertEquals(true, cors.allowHeader("Origin"));
          assertEquals(true,
              cors.allowHeaders(asList("X-Requested-With", "Content-Type", "Accept", "Origin")));
          assertEquals(false,
              cors.allowHeaders(asList("X-Requested-With", "Content-Type", "Custom")));
        });
  }

  private void cors(final Config conf, final Consumer<Cors> callback) {
    callback.accept(new Cors(conf));
  }

  private void cors(final Consumer<Cors> callback) {
    callback.accept(new Cors());
  }

  private Config baseconf() {
    Config config = ConfigFactory.empty()
        .withValue("enabled", fromAnyRef(true))
        .withValue("credentials", fromAnyRef(true))
        .withValue("maxAge", fromAnyRef("30m"))
        .withValue("origin", fromAnyRef(Lists.newArrayList()))
        .withValue("exposedHeaders", fromAnyRef(Lists.newArrayList("X")))
        .withValue("allowedMethods", fromAnyRef(Lists.newArrayList()))
        .withValue("allowedHeaders", fromAnyRef(Lists.newArrayList()));
    return config;
  }

}
