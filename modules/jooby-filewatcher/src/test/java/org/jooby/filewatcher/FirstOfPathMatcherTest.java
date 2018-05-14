package org.jooby.filewatcher;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class FirstOfPathMatcherTest {

  @Test
  public void nomatches() {
    assertEquals(false, new FirstOfPathMatcher(ImmutableList.of()).matches(Paths.get("target")));
  }

  @Test
  public void matchFirst() {
    assertEquals(true,
        new FirstOfPathMatcher(
            ImmutableList.of(new GlobPathMatcher("**/*.java")))
                .matches(Paths.get("target/Foo.java")));
    assertEquals(true,
        new FirstOfPathMatcher(
            ImmutableList.of(new GlobPathMatcher("**/*.java")))
                .matches(Paths.get("target/foo/Foo.java")));
    assertEquals(false,
        new FirstOfPathMatcher(
            ImmutableList.of(new GlobPathMatcher("**/*.java")))
                .matches(Paths.get("target/Foo.kt")));
  }

  @Test
  public void matchOne() {
    assertEquals(true,
        new FirstOfPathMatcher(
            ImmutableList.of(new GlobPathMatcher("**/*.java"),
                new GlobPathMatcher("**/*.kt")))
                    .matches(Paths.get("target/Foo.kt")));
  }

}
