/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MapModelAndViewTest {

  @Test
  @DisplayName("Verify constructor with view and provided model")
  void testConstructorWithViewAndModel() {
    Map<String, Object> initialModel = new HashMap<>();
    initialModel.put("key1", "value1");

    MapModelAndView mav = new MapModelAndView("index.html", initialModel);

    assertEquals("index.html", mav.getView());
    assertSame(initialModel, mav.getModel());
  }

  @Test
  @DisplayName("Verify constructor with view only initializes an empty LinkedHashMap")
  void testConstructorWithViewOnly() {
    MapModelAndView mav = new MapModelAndView("index.html");

    assertEquals("index.html", mav.getView());
    assertTrue(mav.getModel().isEmpty());
    assertEquals("java.util.LinkedHashMap", mav.getModel().getClass().getName());
  }

  @Test
  @DisplayName("Verify put(String, Object) adds to model and returns this")
  void testPutSingleAttribute() {
    MapModelAndView mav = new MapModelAndView("index.html");

    MapModelAndView result = mav.put("foo", "bar");

    assertSame(mav, result, "put should return the current instance for fluent chaining");
    assertEquals(1, mav.getModel().size());
    assertEquals("bar", mav.getModel().get("foo"));
  }

  @Test
  @DisplayName("Verify put(Map) adds all attributes to model and returns this")
  void testPutMultipleAttributes() {
    MapModelAndView mav = new MapModelAndView("index.html");

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("item1", 100);
    attributes.put("item2", "text");

    MapModelAndView result = mav.put(attributes);

    assertSame(mav, result, "put should return the current instance for fluent chaining");
    assertEquals(2, mav.getModel().size());
    assertEquals(100, mav.getModel().get("item1"));
    assertEquals("text", mav.getModel().get("item2"));
  }

  @Test
  @DisplayName("Verify setLocale(Locale) delegates to super and returns this")
  void testSetLocale() {
    MapModelAndView mav = new MapModelAndView("index.html");
    Locale locale = Locale.UK;

    MapModelAndView result = mav.setLocale(locale);

    assertSame(mav, result, "setLocale should return the current instance for fluent chaining");
    assertEquals(locale, mav.getLocale());
  }
}
