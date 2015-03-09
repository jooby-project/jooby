package org.jooby;

import java.util.NoSuchElementException;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ErrDefaultStatusCodeFeature extends ServerFeature {

  {
    get("/IllegalArgumentException", (req, rsp) -> {
      throw new IllegalArgumentException();
    });

    get("/NoSuchElementException", (req, rsp) -> {
      throw new NoSuchElementException();
    });

    get("/Err", (req, rsp) -> {
      throw new Err(Status.BAD_REQUEST);
    });

    get("/NullPointerException", (req, rsp) -> {
      throw new NullPointerException();
    });

    get("/NotAcceptable", (req, rsp) -> {
      rsp.send(new Object());
    }).produces("json");

    post("/UnsupportedMediaType", (req, rsp) -> {
      rsp.send(req.body(String.class));
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
