/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class BaseDirTest {
  @Test
  public void findBaseDir() {
    var basedir = Paths.get(System.getProperty("user.dir"));
    var path = JoobyRun.baseDir(basedir, JoobyRun.class);
    assertEquals(basedir, path);
  }
}
