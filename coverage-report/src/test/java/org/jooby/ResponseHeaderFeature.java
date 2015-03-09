package org.jooby;

import java.util.Date;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class ResponseHeaderFeature extends ServerFeature {

  {
    get("/headers", (req, rsp) -> {
      rsp.header("byte", (byte) 7);
      rsp.header("char", 'c');
      rsp.header("double", 7.0d);
      rsp.header("float", 2.1f);
      rsp.header("int", 4);
      rsp.header("long", 2L);
      rsp.header("date", new Date(1416700709415L));
      rsp.header("short", (short) 43);
      rsp.header("str", "str");
      rsp.status(200);
    });
  }

  @Test
  public void headers() throws Exception {
    request()
        .get("/headers")
        .expect(200)
        .header("byte", 7)
        .header("char", 'c')
        .header("float", 2.1)
        .header("double", 7.0)
        .header("int", 4)
        .header("long", 2)
        .header("date", "Sat, 22 Nov 2014 23:58:29 GMT")
        .header("short", 43)
        .header("str", "str");
  }

}
