package io.jooby.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "Creates a new application")
public class CreateApp implements Runnable {
  @CommandLine.Parameters
  private String name;

  @CommandLine.Option(names = {"-m", "--maven"}, defaultValue = "true")
  private boolean maven;

  @CommandLine.Option(names = {"-g", "--gradle"})
  private boolean gradle;

  @Override public void run() {
    System.out.println(name);
    System.out.println(maven);
    System.out.println(gradle);
  }
}
