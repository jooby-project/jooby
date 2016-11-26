package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Status;
import org.junit.Test;

public class Issue576 {

  @Test
  public void shouldThrowBootstrapException() {
    IllegalStateException ies = new IllegalStateException("boot err");
    try {
      new Jooby() {
        {
          throwBootstrapException();

          onStart(() -> {
            throw ies;
          });
        }
      }.start();
      fail();
    } catch (Err err) {
      assertEquals(Status.SERVICE_UNAVAILABLE.value(), err.statusCode());
      assertEquals(ies, err.getCause());
    }
  }

}
