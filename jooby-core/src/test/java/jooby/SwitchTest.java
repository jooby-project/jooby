package jooby;

import static org.junit.Assert.assertEquals;
import jooby.fn.Switches;

import org.junit.Test;

public class SwitchTest {

  @Test
  public void keyValue() throws Exception {
    assertEquals("X", Switches.newSwitch("x")
        .when("x", "X")
        .when("y", "Y")
        .get());

    assertEquals("Y", Switches.newSwitch("y")
        .when("x", "X")
        .when("y", "Y")
        .get());

    assertEquals("Xx", Switches.newSwitch("x")
        .when("x", "Xx")
        .when("x", "Y")
        .get());
  }

  @Test
  public void keyFn() throws Exception {
    assertEquals("X", Switches.newSwitch("x")
        .when("x", () -> "X")
        .when("y", () -> "Y")
        .get());

    assertEquals("Y", Switches.newSwitch("y")
        .when("x", () -> "X")
        .when("y", () -> "Y")
        .get());

    assertEquals("Xx", Switches.newSwitch("x")
        .when("x", () -> "Xx")
        .when("x", () -> "Y")
        .get());
  }

  @Test
  public void predicateFn() throws Exception {
    assertEquals("X", Switches.newSwitch("x")
        .when((v) -> "x".equals(v), () -> "X")
        .when((v) -> "y".equals(v), () -> "Y")
        .get());

    assertEquals("Y", Switches.newSwitch("y")
        .when((v) -> "x".equals(v), () -> "X")
        .when((v) -> "y".equals(v), () -> "Y")
        .get());

    assertEquals("Xx", Switches.newSwitch("x")
        .when((v) -> "x".equals(v), () -> "Xx")
        .when((v) -> "x".equals(v), () -> "Y")
        .get());
  }

  @Test
  public void predicateValue() throws Exception {
    assertEquals("X", Switches.newSwitch("x")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .get());

    assertEquals("Y", Switches.newSwitch("y")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .get());

    assertEquals("Xx", Switches.newSwitch("x")
        .when((v) -> "x".equals(v), "Xx")
        .when((v) -> "x".equals(v), "Y")
        .get());
  }

  @Test
  public void otherwise() throws Exception {
    assertEquals("zz", Switches.newSwitch("z")
        .when((v) -> "x".equals(v), "X")
        .when((v) -> "y".equals(v), "Y")
        .otherwise("zz"));

  }
}
