package org.jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ViewTest {

  static class ViewTestEngine implements View.Engine {

    @Override
    public void render(final View viewable, final Context ctx) throws Exception {
      // TODO Auto-generated method stub

    }

  }

  @Test
  public void viewOnly() {
    View view = Results.html("v");
    assertEquals("v", view.name());
    assertEquals(0, view.model().size());
  }

  @Test
  public void viewWithDefModel() {
    View view = Results.html("v").put("m", "x");
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("x", view.model().get("m"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void failOnSet() {
    View view = Results.html("v").put("m", "x");
    view.set(view);
  }

  @Test
  public void viewBuildModel() {
    View view = Results.html("v").put("m", "x");
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("x", view.model().get("m"));
  }

  @Test
  public void viewBuildModelMap() {
    View view = Results.html("v").put("m", ImmutableMap.of("k", "v"));
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals(ImmutableMap.of("k", "v"), view.model().get("m"));
  }

  @Test
  public void viewPutMap() {
    View view = Results.html("v").put(ImmutableMap.of("k", "v"));
    assertEquals("v", view.name());
    assertEquals(1, view.model().size());
    assertEquals("v", view.model().get("k"));
  }


}
