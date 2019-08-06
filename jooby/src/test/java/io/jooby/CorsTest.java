package io.jooby;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CorsTest {

  @Test
  public void defaults() {
    cors(cors -> {
      assertEquals(true, cors.anyOrigin());
      assertEquals(Arrays.asList("*"), cors.getOrigin());
      assertEquals(true, cors.getUseCredentials());

      assertEquals(true, cors.allowMethod("get"));
      assertEquals(true, cors.allowMethod("post"));
      assertEquals(Arrays.asList("GET", "POST"), cors.getMethods());

      assertEquals(true, cors.allowHeader("X-Requested-With"));
      assertEquals(true, cors.allowHeader("Content-Type"));
      assertEquals(true, cors.allowHeader("Accept"));
      assertEquals(true, cors.allowHeader("Origin"));
      assertEquals(true, cors.allowHeader("X-Requested-With", "Content-Type", "Accept", "Origin"));
      assertEquals(Arrays.asList("X-Requested-With", "Content-Type", "Accept", "Origin"),
          cors.getHeaders());

      assertEquals(Duration.ofMinutes(30), cors.getMaxAge());

      assertEquals(Arrays.asList(), cors.getExposedHeaders());

      assertEquals(false, cors.setUseCredentials(false).getUseCredentials());
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
    cors(baseconf().withValue("methods", fromAnyRef("GET")), cors -> {
      assertEquals(true, cors.allowMethod("GET"));
      assertEquals(true, cors.allowMethod("get"));
      assertEquals(false, cors.allowMethod("POST"));
    });

    cors(baseconf().withValue("methods", fromAnyRef(asList("get", "post"))), cors -> {
      assertEquals(true, cors.allowMethod("GET"));
      assertEquals(true, cors.allowMethod("get"));
      assertEquals(true, cors.allowMethod("POST"));
    });
  }

  @Test
  public void requestHeaders() {
    cors(baseconf().withValue("headers", fromAnyRef("*")), cors -> {
      assertEquals(true, cors.anyHeader());
      assertEquals(true, cors.allowHeader("Custom-Header"));
    });

    cors(baseconf().withValue("headers", fromAnyRef(asList("X-Requested-With", "*"))),
        cors -> {
          assertEquals(true, cors.allowHeader("X-Requested-With"));
          assertEquals(true, cors.anyHeader());
        });

    cors(
        baseconf().withValue("headers",
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
    callback.accept(Cors.from(conf));
  }

  private void cors(final Consumer<Cors> callback) {
    callback.accept(new Cors());
  }

  private Config baseconf() {
    Config config = ConfigFactory.empty()
        .withValue("credentials", fromAnyRef(true))
        .withValue("maxAge", fromAnyRef("30m"))
        .withValue("origin", fromAnyRef(Lists.newArrayList()))
        .withValue("exposedHeaders", fromAnyRef(Lists.newArrayList("X")))
        .withValue("methods", fromAnyRef(Lists.newArrayList()))
        .withValue("headers", fromAnyRef(Lists.newArrayList()));
    return config;
  }

}
