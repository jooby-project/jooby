/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Issue2369 {

  @BeforeEach
  @AfterEach
  public void cleanState() {
    Server.Base.clearState();
  }

  @Test
  public void shouldCustomizeServerLostException() {
    Throwable cause = new IllegalArgumentException();

    Server.addConnectionLost(it -> it == cause);

    assertTrue(Server.connectionLost(cause));
    assertFalse(Server.connectionLost(new IllegalArgumentException()));
  }

  @Test
  public void shouldCustomizeAddressInUseException() {
    Throwable cause = new IllegalArgumentException();

    Server.addAddressInUse(it -> it == cause);

    assertTrue(Server.isAddressInUse(cause));
    assertFalse(Server.isAddressInUse(new IllegalArgumentException()));
  }
}
