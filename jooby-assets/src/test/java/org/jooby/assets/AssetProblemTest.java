package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssetProblemTest {

  @Test
  public void defaults() {
    AssetProblem problem = new AssetProblem("x.js", 5, 2, "message", null);
    assertEquals(2, problem.getColumn());
    assertEquals("x.js", problem.getFilename());
    assertEquals(5, problem.getLine());
    assertEquals("message", problem.getMessage());
    assertEquals("", problem.getEvidence());
    assertEquals("x.js:5:2: message", problem.toString());
  }

  @Test
  public void withEvidenve() {
    AssetProblem problem = new AssetProblem("x.js", 5, 2, "message", "evidence");
    assertEquals(2, problem.getColumn());
    assertEquals("x.js", problem.getFilename());
    assertEquals(5, problem.getLine());
    assertEquals("message", problem.getMessage());
    assertEquals("evidence", problem.getEvidence());
    assertEquals("x.js:5:2: message\nevidence", problem.toString());
  }
}
