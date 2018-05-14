package org.jooby;

import java.util.NoSuchElementException;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ErrDefaultStatusCodeFeature extends ServerFeature {

  {
    get("/IllegalArgumentException", (req, rsp) -> {
      throw new IllegalArgumentException("intentional err");
    });

    get("/NoSuchElementException", (req, rsp) -> {
      throw new NoSuchElementException("intentional err");
    });

    get("/Err", (req, rsp) -> {
      throw new Err(Status.BAD_REQUEST, "intentional err");
    });

    get("/NullPointerException", (req, rsp) -> {
      throw new NullPointerException("intentional err");
    });

    get("/NotAcceptable", (req, rsp) -> {
      rsp.send(new Object());
    }).produces("json");

    post("/UnsupportedMediaType", (req, rsp) -> {
      rsp.send(req.body().to(String.class));
    }).consumes("json");

  }

  @Test
  public void errIllegalArgumentException() throws Exception {
    request()
        .get("/IllegalArgumentException")
        .expect(400);
  }

  @Test
  public void errNoSuchElementException() throws Exception {
    request()
        .get("/NoSuchElementException")
        .expect(400);
  }

  @Test
  public void errNullPointerException() throws Exception {
    request()
        .get("/NullPointerException")
        .expect(500);
  }

  @Test
  public void err() throws Exception {
    request()
        .get("/Err")
        .expect(400);
  }

  @Test
  public void NotAcceptable() throws Exception {
    request()
        .get("/NotAcceptable")
        .header("Accept", "text/html")
        .expect(406);
  }

  @Test
  public void unsupportedMediaType() throws Exception {
    request()
        .post("/UnsupportedMediaType")
        .body("<xml><form></form>", "application/xml")
        .expect(415);
  }

}
