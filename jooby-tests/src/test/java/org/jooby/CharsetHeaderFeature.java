package org.jooby;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.fluent.Request;
import org.junit.Test;

import com.google.common.base.Charsets;

public class CharsetHeaderFeature extends ServerFeature {

  {
    get("/", (req, res) -> res.send(req.charset()));
  }

  @Test
  public void defCharset() throws Exception {
    assertEquals("UTF-8", Request.Get(uri("/").build()).execute().returnContent().asString());
  }

  @Test
  public void customCharset() throws Exception {
    assertEquals("ISO-8859-1",
        Request.Get(uri("/").build())
            .addHeader("Content-Type", "text/html; charset=" + Charsets.ISO_8859_1.name())
            .execute().returnContent().asString());
  }
}
