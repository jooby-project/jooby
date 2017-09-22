package org.jooby.internal;

import org.jooby.Err;
import org.jooby.Status;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class EmptyBodyReferenceTest {

  @Test
  public void bytes() throws Throwable {
    badRequest(EmptyBodyReference::bytes);
  }

  @Test
  public void text() throws Throwable {
    badRequest(EmptyBodyReference::text);
  }

  @Test
  public void writeTo() throws Throwable {
    badRequest(e -> e.writeTo(null));
  }

  @Test
  public void len() throws Throwable {
    assertEquals(0, new EmptyBodyReference().length());
  }

  private void badRequest(final Throwing.Consumer<EmptyBodyReference> callback) throws Throwable {
    try {
      callback.accept(new EmptyBodyReference());
      fail();
    } catch (Err x) {
      assertEquals(Status.BAD_REQUEST.value(), x.statusCode());
    }
  }
}
