package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.Lists;

public class AssetExceptionTest {

  @Test
  public void defaults() {
    AssetProblem problem = new AssetProblem("x.js", 5, 2, "message", null);
    AssetException ex = new AssetException("x", problem);
    assertEquals("x", ex.getId());
    assertEquals(Lists.newArrayList(problem), ex.getProblems());
  }
}
