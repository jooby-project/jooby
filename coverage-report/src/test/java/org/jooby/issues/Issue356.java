package org.jooby.issues;

import org.jooby.handlers.AssetHandler;
import org.jooby.test.ServerFeature;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class Issue356 extends ServerFeature {
  {
    assets("/assets/file.css", new AssetHandler("/")
      .etag(false).lastModified(false).maxAge(86400));

    assets("/assets/favicon.ico", new AssetHandler("/")
      .etag(false).lastModified(true).maxAge(1209600));

    assets("/assets/file.js", new AssetHandler("/")
      .etag(true).lastModified(false).maxAge(172800));

    assets("/assets/empty.css", new AssetHandler("/")
      .etag(true).lastModified(true).maxAge(604800));
  }

  @Test
  public void etag() throws Exception {
    request().get("/assets/file.css").execute().header("ETag", Assert::assertNull);
    request().get("/assets/favicon.ico").execute().header("ETag", Assert::assertNull);
    request().get("/assets/file.js").execute().header("ETag", Assert::assertNotNull);
    request().get("/assets/empty.css").execute().header("ETag", Assert::assertNotNull);
  }

  @Test
  public void lastModified() throws Exception {
    request().get("/assets/file.css").execute().header("Last-Modified", Assert::assertNull);
    request().get("/assets/favicon.ico").execute().header("Last-Modified", Assert::assertNotNull);
    request().get("/assets/file.js").execute().header("Last-Modified", Assert::assertNull);
    request().get("/assets/empty.css").execute().header("Last-Modified", Assert::assertNotNull);
  }

  @Test
  public void maxAge() throws Exception {
    request().get("/assets/file.css").execute().header("Cache-Control", value -> {
      assertNotNull(value);
      assertTrue(value.contains("max-age=86400"));
    });
    request().get("/assets/favicon.ico").execute().header("Cache-Control", value -> {
      assertNotNull(value);
      assertTrue(value.contains("max-age=1209600"));
    });
    request().get("/assets/file.js").execute().header("Cache-Control", value -> {
      assertNotNull(value);
      assertTrue(value.contains("max-age=172800"));
    });
    request().get("/assets/empty.css").execute().header("Cache-Control", value -> {
      assertNotNull(value);
      assertTrue(value.contains("max-age=604800"));
    });
  }
}
