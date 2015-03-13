package org.jooby;

import static org.junit.Assert.assertEquals;

import org.jooby.util.Switch;
import org.junit.Test;

public class SwitchTest {

  @Test
  public void keyValue() throws Exception {

    assertEquals("X", Switch.newSwitch("x")
        .when("x", "X")
        .when("y", "Y")
        .value().get());

    assertEquals("Y", Switch.newSwitch("y")
        .when("x", "X")
        .when("y", "Y")
        .value().get());

    assertEquals("Xx", Switch.newSwitch("x")
        .when("x", "Xx")
        .when("x", "Y")
        .value().get());
  }

  @Test
  public void keyFn() throws Exception {
    assertEquals("X", Switch.newSwitch("x")
        .when("x", () -> "X")
        .when("y", () -> "Y")
        .value().get());

    assertEquals("Y", Switch.newSwitch("y")
        .when("x", () -> "X")
        .when("y", () -> "Y")
        .value().get());

    assertEquals("Xx", Switch.newSwitch("x")
        .when("x", () -> "Xx")
        .when("x", () -> "Y")
        .value().get());
  }

  @Test
  public void predicateFn() throws Exception {
    assertEquals("X", Switch.newSwitch("x")
        .when((v) -> "x".equals(v), () -> "X")
        .when((v) -> "y".equals(v), () -> "Y")
        .value().get());

    assertEquals("Y", Switch.newSwitch("y")
        .when((v) -> "x".equals(v), () -> "X")
        .when((v) -> "y".equals(v), () -> "Y")
        .value().get());

    assertEquals("Xx", Switch.newSwitch("x")
        .when((v) -> "x".equals(v), () -> "Xx")
        .when((v) -> "x".equals(v), () -> "Y")
        .value().get());
  }

  @Test
  public void predicateValue() throws Exception {
    assertEquals("X", Switch.newSwitch("x")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .value().get());

    assertEquals("Y", Switch.newSwitch("y")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .value().get());

    assertEquals("Xx", Switch.newSwitch("x")
        .when((v) -> "x".equals(v), "Xx")
        .when((v) -> "x".equals(v), "Y")
        .value().get());
  }

  @Test
  public void otherwise() throws Exception {
    assertEquals("zz", Switch.newSwitch("z")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .value()
        .orElse("zz"));

  }
}
