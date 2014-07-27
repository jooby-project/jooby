package jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

public class AssetFeature extends ServerFeature {

  {
    assets("/assets/**");
  }

  @Test
  public void jsAsset() throws Exception {
    HttpResponse response = Request.Get(uri("/assets/file.js").build()).execute().returnResponse();
    assertEquals("application/javascript; charset=UTF-8", response.getFirstHeader("Content-Type")
        .getValue());
    assertEquals(200, response.getStatusLine().getStatusCode());
    String lastModified = response.getFirstHeader("Last-Modified").getValue();

    response = Request.Get(uri("/assets/file.js").build())
        .addHeader("If-Modified-Since", lastModified).execute().returnResponse();
    assertEquals(304, response.getStatusLine().getStatusCode());
  }

  @Test
  public void cssAsset() throws Exception {
    HttpResponse response = Request.Get(uri("/assets/file.css").build()).execute().returnResponse();
    assertEquals("text/css; charset=UTF-8", response.getFirstHeader("Content-Type")
        .getValue());
    assertEquals(200, response.getStatusLine().getStatusCode());
    String lastModified = response.getFirstHeader("Last-Modified").getValue();

    response = Request.Get(uri("/assets/file.css").build())
        .addHeader("If-Modified-Since", lastModified).execute().returnResponse();
    assertEquals(304, response.getStatusLine().getStatusCode());
  }

}
