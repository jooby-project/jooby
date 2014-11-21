package org.jooby.integration;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
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
    public String locale(final org.jooby.Request req) {
      return charset.toString();
    }
  }

  {
    use(ConfigFactory.empty().withValue("application.charset", ConfigValueFactory.fromAnyRef(Charsets.ISO_8859_1.name())));
    use(Resource.class);
  }

  @Test
  public void charset() throws Exception {
    assertEquals("ISO-8859-1", execute(GET(uri("/")), (response) -> {
      assertEquals(200, response.getStatusLine().getStatusCode());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static Object execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
