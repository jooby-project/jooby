/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SessionTest {

  private Session session;

  @BeforeEach
  void setUp() {
    // We mock the interface to test default methods
    session = mock(Session.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void putOverloads() {
    // int
    session.put("k1", 100);
    verify(session).put("k1", "100");

    // long
    session.put("k2", 200L);
    verify(session).put("k2", "200");

    // float
    session.put("k3", 1.5f);
    verify(session).put("k3", "1.5");

    // double
    session.put("k4", 2.5d);
    verify(session).put("k4", "2.5");

    // boolean
    session.put("k5", true);
    verify(session).put("k5", "true");

    // CharSequence
    session.put("k6", new StringBuilder("hello"));
    verify(session).put("k6", "hello");

    // Number
    session.put("k7", (Number) 500);
    verify(session).put("k7", "500");
  }

  @Test
  void staticCreate() {
    Context ctx = mock(Context.class);
    when(ctx.getValueFactory()).thenReturn(mock(io.jooby.value.ValueFactory.class));

    // create(ctx, id)
    Session s1 = Session.create(ctx, "123");
    assertNotNull(s1);
    assertEquals("123", s1.getId());

    // create(ctx, id, data)
    Map<String, String> data = new HashMap<>();
    data.put("foo", "bar");
    Session s2 = Session.create(ctx, "456", data);
    assertNotNull(s2);
    assertEquals("456", s2.getId());
    assertEquals("bar", s2.get("foo").value());
  }

  @Test
  void constants() {
    assertEquals("session", Session.NAME);
  }
}
