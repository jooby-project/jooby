package org.jooby;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class CharsetFeature extends ServerFeature {

  public static class Resource {

    private Charset charset;

    @Inject
    public Resource(@Named("application.charset") final Charset namedCharset, final Charset charset) {
      this.charset = requireNonNull(charset, "def charset is required.");
      assertEquals(charset, namedCharset);
    }

    @GET
    @Path("/")
    public byte[] charset(final Optional<Charset> charset) {
      Charset cs = charset.orElse(this.charset);
      return cs.name().getBytes(cs);
    }
  }

  {
    use(ConfigFactory
        .empty()
        .withValue("application.charset", ConfigValueFactory.fromAnyRef(Charsets.ISO_8859_1.name())));
    use(Resource.class);
  }

  @Test
  public void charset() throws Exception {
    request()
        .get("/")
        .expect("ISO-8859-1")
        .header("Content-Length", "10")
        .expect(200);

    request()
        .get("/?charset=UTF-8")
        .expect("UTF-8")
        .header("Content-Length", "5")
        .expect(200);
  }

}
