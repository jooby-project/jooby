/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * Set/configure options used by the tool (for now is just for workspace).
 *
 * @since 2.0.6
 */
@CommandLine.Command(name = "set", description = "Set and save options in the ~/.jooby file")
public class SetCmd extends Cmd {
  @CommandLine.Spec
  private CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-w", "--workspace"}, description = "Save the workspace directory. Projects are going to be created here")
  private Path workspace;

  @Override public void run(@Nonnull Context ctx) throws Exception {
    if (workspace != null) {
      ctx.setWorkspace(workspace);
    } else {
      ctx.println(spec.commandLine().getUsageMessage());
    }
  }
}
