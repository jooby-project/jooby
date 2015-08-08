package org.jooby.internal.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Parameter;

import javax.inject.Named;

import org.jooby.mvc.Header;
import org.junit.Test;

public class RequestParamTest {

  public void javax(@Named("javax") final String s) {

  }

  public void ejavax(@Named final String s) {
  }

  public void guice(@com.google.inject.name.Named("guice") final String s) {
  }

  public void header(@Header("H-1") final String s) {
  }

  public void namedheader(@Named("x") @Header final String s) {
  }

  public void eheader(@Header final String s) {
  }

  @Test
  public void name() throws Exception {
    assertEquals("javax", RequestParam.nameFor(param("javax")));

    assertTrue(RequestParam.nameFor(param("ejavax")) == null
        || "s".equals(RequestParam.nameFor(param("ejavax"))));

    assertEquals("guice", RequestParam.nameFor(param("guice")));

    assertEquals("H-1", RequestParam.nameFor(param("header")));

    assertEquals("x", RequestParam.nameFor(param("namedheader")));

    assertTrue(RequestParam.nameFor(param("eheader")) == null
        || "s".equals(RequestParam.nameFor(param("eheader"))));

  }

  private Parameter param(final String name) throws Exception {
    return RequestParamTest.class.getDeclaredMethod(name, String.class).getParameters()[0];
  }
}
