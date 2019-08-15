package io.jooby.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "jooby", mixinStandardHelpOptions = true, description = "jooby console", subcommands = {
    CreateApp.class})
public class Cli implements Runnable {
  @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  public void run() {
    throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Cli())
        .execute(args);
    System.exit(exitCode);
  }
}
