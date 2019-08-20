/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import picocli.CommandLine;

import javax.annotation.Nonnull;

/**
 * Exit console application.
 *
 * @since 2.0.6
 */
@CommandLine.Command(name = "exit", description = "Exit console")
public class Exit extends Command {

  @Override public void run(@Nonnull CommandContext ctx) {
    ctx.exit(0);
  }
}
