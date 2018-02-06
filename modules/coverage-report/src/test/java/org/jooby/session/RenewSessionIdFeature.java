package org.jooby.session;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.jooby.test.Client;
import org.jooby.test.ServerFeature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RenewSessionIdFeature extends ServerFeature {
  {
    get("/991/createSession", req -> {
      req.session().set("foo", "bar");
      return req.session().attributes();
    });

    get("/991/sessionAttributes", req -> {
      return req.session().attributes();
    });

    get("/991/renewSession", req -> {
      req.session().renewId();
      return req.session().attributes();
    });
  }

  @Test
  public void shouldRenewSessionId() throws Exception {
    List<String> sid = new ArrayList<>();

    Client.Callback cookieHandler = value -> {
      List<String> setCookie = Lists.newArrayList(Splitter.onPattern(";\\s*")
          .splitToList(value));
      sid.add(setCookie.get(0));
      assertTrue(setCookie.remove(0).startsWith("jooby.sid"));
      assertTrue(setCookie.remove("Path=/") || setCookie.remove("Path=/"));
      assertTrue(setCookie.remove("HttpOnly") || setCookie.remove("HTTPOnly"));
      assertTrue(setCookie.remove("Version=1"));
    };

    request()
        .get("/991/createSession")
        .expect(200)
        .header("Set-Cookie", cookieHandler);

    request()
        .get("/991/renewSession")
        .header("Cookie", sid.get(0))
        .expect(200)
        .header("Set-Cookie", cookieHandler);

    assertEquals(2, sid.size());
    assertNotEquals(sid.get(0), sid.get(1));
  }
}
