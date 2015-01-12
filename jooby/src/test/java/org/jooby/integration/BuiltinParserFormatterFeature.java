package org.jooby.integration;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jooby.integration.FilterFeature.HttpResponseValidator;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class BuiltinParserFormatterFeature extends ServerFeature {

  {
    get("/stream", () -> new ByteArrayInputStream("stream".getBytes()));

    get("/bytes", () -> "bytes".getBytes());

    get("/direct-buffer", () -> {
      ByteBuffer buffer = ByteBuffer.allocateDirect("direct-buffer".length());
      buffer.put("direct-buffer".getBytes());
      return buffer;
    });

    get("/reader", () -> new StringReader("reader"));
  }

  @Test
  public void stream() throws Exception {
    assertEquals("stream", execute(GET(uri("stream")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertEquals("application/octet-stream", rsp.getFirstHeader("Content-Type").getValue());
    }));
  }

  @Test
  public void bytes() throws Exception {
    assertEquals("bytes", execute(GET(uri("bytes")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertEquals("application/octet-stream", rsp.getFirstHeader("Content-Type").getValue());
    }));
  }

  @Test
  public void directBuffer() throws Exception {
    assertEquals("direct-buffer", execute(GET(uri("direct-buffer")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertEquals("application/octet-stream", rsp.getFirstHeader("Content-Type").getValue());
    }));
  }

  @Test
  public void reader() throws Exception {
    assertEquals("reader", execute(GET(uri("reader")), rsp -> {
      assertEquals(200, rsp.getStatusLine().getStatusCode());
      assertEquals("text/html;charset=UTF-8", rsp.getFirstHeader("Content-Type").getValue());
    }));
  }

  private static Request GET(final URIBuilder uri) throws Exception {
    return Request.Get(uri.build());
  }

  private static String execute(final Request request, final HttpResponseValidator validator)
      throws Exception {
    HttpResponse resp = request.execute().returnResponse();
    validator.validate(resp);
    return EntityUtils.toString(resp.getEntity());
  }
}
