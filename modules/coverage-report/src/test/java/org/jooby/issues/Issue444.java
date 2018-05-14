package org.jooby.issues;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue444 extends ServerFeature {

  {

    parser((type, ctxt) -> {
      if (type.getRawType() == ByteBuffer.class) {
        return ctxt.body(body -> {
          return ByteBuffer.wrap(body.bytes());
        });
      }
      return ctxt.next();
    });

    post("/444", req -> {
      ByteBuffer buffer = req.body(ByteBuffer.class);
      return buffer.remaining();
    });

    post("/444/raw", req -> {
      return req.body(String.class);
    });
  }

  @Test
  public void shouldAcceptPostWithoutContentType() throws URISyntaxException, Exception {
    request()
        .post("/444")
        .body("abc", null)
        .expect("3");
  }

  @Test
  public void shouldFavorCustomParserOnFormUrlEncoded() throws URISyntaxException, Exception {
    request()
        .post("/444")
        .body("abc", "application/x-www-form-urlencoded")
        .expect("3");
  }

  @Test
  public void shouldFavorCustomParserOnMultipart() throws URISyntaxException, Exception {
    request()
        .post("/444")
        .body("abc", "multipart/form-data")
        .expect("3");
  }

  @Test
  public void shouldGetRawBody() throws URISyntaxException, Exception {
    request()
        .post("/444/raw")
        .form()
        .add("foo", "bar")
        .add("bar", "foo")
        .expect("foo=bar&bar=foo");

    request()
        .post("/444/raw")
        .body("--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK\n" +
            "Content-Disposition: form-data; name=\"foo\"\n" +
            "Content-Type: text/plain\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "\n" +
            "bar\n" +
            "--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK\n" +
            "Content-Disposition: form-data; name=\"bar\"; filename=\"foo.txt\"\n" +
            "Content-Type: text/plain\n" +
            "Content-Transfer-Encoding: binary\n" +
            "\n" +
            "foo\n" +
            "--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK--", "multipart/form-data")
        .expect("--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK\n" +
            "Content-Disposition: form-data; name=\"foo\"\n" +
            "Content-Type: text/plain\n" +
            "Content-Transfer-Encoding: 8bit\n" +
            "\n" +
            "bar\n" +
            "--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK\n" +
            "Content-Disposition: form-data; name=\"bar\"; filename=\"foo.txt\"\n" +
            "Content-Type: text/plain\n" +
            "Content-Transfer-Encoding: binary\n" +
            "\n" +
            "foo\n" +
            "--ylYSWaNWL2lXy3vBYw458nuB9UDehD5o6iHZuLK--");
  }

}
