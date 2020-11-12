/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import picocli.CommandLine;

/**
 * Set/configure options used by the tool (for now is just for workspace).
 *
 * @since 2.0.6
 */
@CommandLine.Command(name = "set", description = "Set and save options in the ~/.jooby file")
public class SetCmd extends Cmd {
  @CommandLine.Spec
  private CommandLine.Model.CommandSpec spec;

  @CommandLine.Option(names = {"-w",
      "--workspace"}, description = "Save the workspace directory. Projects are going to be created here")
  private String workspace;

  @CommandLine.Option(names = {"-f",
      "--force"}, description = "Force creation of workspace")
  private boolean force;

  @Override public void run(@Nonnull Context ctx) throws Exception {
    if (workspace != null) {
      Path path = Paths.get(
          workspace.replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home"))))
          .normalize()
          .toAbsolutePath();
      if (Files.exists(path)) {
        ctx.setWorkspace(path);
      } else {
        if (force) {
          Files.createDirectories(path);
          ctx.setWorkspace(path);
        } else {
          ctx.println("Directory doesn't exist: " + path);
          ctx.println("Use -f to force directory creation");
        }
      }
    } else {
      ctx.println(spec.commandLine().getUsageMessage());
    }
  }
}
