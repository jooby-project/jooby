/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import picocli.CommandLine;

public class CliTest {

  @Test
  public void rewriteCommand() {
    var cmdLine = Mockito.mock(CommandLine.class);
    var command = Cli.rewrite(cmdLine, "jooby-code-gen", "--mvc");
    assertArrayEquals(new String[] {"create", "jooby-code-gen", "--mvc"}, command);
  }
}
