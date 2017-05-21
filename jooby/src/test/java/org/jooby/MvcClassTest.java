package org.jooby;

import static org.junit.Assert.assertEquals;

import org.jooby.Jooby.MvcClass;
import org.jooby.Route.Definition;
import org.junit.Test;

public class MvcClassTest {

  @Test
  public void rendererAttr() throws Exception {
    MvcClass mvcClass = new Jooby.MvcClass(MvcClassTest.class, "/", null);
    mvcClass.renderer("text");
    assertEquals("text", mvcClass.renderer());
    Definition route = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    });
    mvcClass.apply(route);
    assertEquals("text", route.renderer());
  }
}
