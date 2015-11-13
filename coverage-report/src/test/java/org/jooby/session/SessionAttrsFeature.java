package org.jooby.session;

import static org.junit.Assert.assertEquals;

import org.jooby.Session;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class SessionAttrsFeature extends ServerFeature {

  {

    get("/attrs", req -> {
      Session session = req.session();
      session.set("bool", true);
      assertEquals(true, session.get("bool").booleanValue());
      session.set("byte", (byte) 7);
      assertEquals((byte) 7, session.get("byte").byteValue());
      session.set("c", 'c');
      assertEquals('c', session.get("c").charValue());
      session.set("seq", new StringBuilder("seq"));
      assertEquals("seq", session.get("seq").value());
      session.set("d", 6d);
      assertEquals(6d, session.get("d").doubleValue(), 0);
      session.set("f", 31.9f);
      assertEquals(31.9f, session.get("f").floatValue(), 0);
      session.set("i", 78);
      assertEquals(78, session.get("i").intValue());
      session.set("l", 9L);
      assertEquals(9L, session.get("l").longValue());
      session.set("s", (short) 2);
      assertEquals((short) 2, session.get("s").shortValue());
      return "done";
    });

  }

  @Test
  public void locals() throws Exception {
    request()
        .get("/attrs")
        .expect("done");

  }

}
