package org.jooby;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jooby.Session.Store.SaveReason;
import org.junit.Test;

public class SessionNoopTest {

  @Test
  public void noop() throws Exception {
    new MockUnit(Session.Builder.class, Session.class)
        .run(unit -> {
          Session.Store.NOOP.delete("id");
          assertNotNull(Session.Store.NOOP.generateID(0));
          assertNull(Session.Store.NOOP.get(unit.get(Session.Builder.class)));
          Session.Store.NOOP.save(unit.get(Session.class), SaveReason.NEW);
        });
  }
}
