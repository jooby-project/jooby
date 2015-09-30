package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssetProblemTest {

  @Test
  public void defaults() {
    AssetProblem problem = new AssetProblem("x.js", 5, 2, "message");
    assertEquals(2, problem.getColumn());
    assertEquals("x.js", problem.getFilename());
    assertEquals(5, problem.getLine());
    assertEquals("message", problem.getMessage());
    assertEquals("x.js:5:2: message", problem.toString());
  }
}
