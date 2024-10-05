/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jetty;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

public class Issue3554 {

  @Test
  public void shouldCloseOutputStreamOnce() throws IOException {
    var out = mock(OutputStream.class);
    var ctx = mock(JettyContext.class);

    var jettyOutputStream = new JettyOutputStream(out, ctx);
    jettyOutputStream.close();
    jettyOutputStream.close();

    verify(out, times(1)).close();
  }
}
