package org.jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ViewTest {

  @Test
  public void viewOnly() {
    View view = View.of("v");
    assertEquals("v", view.name());
    assertEquals("", view.engine());
    assertEquals(0, view.model().size());
  }

  @Test
  public void viewWithDefModel() {
    View view = View.of("v", "m", "x");
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("x", view.model().get("m"));
  }

  @Test
  public void viewBuildModel() {
    View view = View.of("v").put("m", "x");
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("x", view.model().get("m"));
  }

  @Test
  public void viewBuildModelMap() {
    View view = View.of("v").put("m", ImmutableMap.of("k", "v"));
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals(ImmutableMap.of("k", "v"), view.model().get("m"));
  }

  @Test
  public void viewPutMap() {
    View view = View.of("v").put(ImmutableMap.of("k", "v"));
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("v", view.model().get("k"));
  }

}
