/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "jooby",
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true,
    version = "Print version information"
)
public class Cli extends Command {
  @CommandLine.Spec CommandLine.Model.CommandSpec spec;
  @CommandLine.Unmatched List<String> args;

  @Override public void run(CommandContext ctx) throws Exception {
    List<String> args = this.args.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(it -> it.length() > 0)
        .collect(Collectors.toList());
    if (args.size() > 0) {
      String arg = args.get(0);
      if ("-h".equals(arg) || "--help".equals(arg)) {
        ctx.out.println(spec.commandLine().getUsageMessage());
      } else if ("-V".equalsIgnoreCase(arg) || "--version".equals(arg)) {
        ctx.out.println(VersionProvider.version());
      } else {
        ctx.out.println(
            "Unknown command or option(s): " + args.stream().collect(Collectors.joining(" ")));
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // set up the completion
    Cli jooby = new Cli();
    CommandLine cmd = new CommandLine(jooby)
        .addSubcommand(new CreateApp())
        .addSubcommand(new Exit());

    Terminal terminal = TerminalBuilder.builder().build();
    LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(new JLineCompleter(cmd.getCommandSpec()))
        .parser(new DefaultParser())
        .build();

    CommandContext context = new CommandContext(reader);
    jooby.setContext(context);
    cmd.getSubcommands().values().stream()
        .map(CommandLine::getCommand)
        .filter(Command.class::isInstance)
        .map(Command.class::cast)
        .forEach(command -> command.setContext(context));

    if (args.length > 0) {
      cmd.execute(args);
    } else {
      String prompt = "jooby> ";

      // start the shell and process input until the user quits with Ctl-D
      while (true) {
        try {
          String line = reader.readLine(prompt);
          ParsedLine pl = reader.getParser().parse(line, 0);
          String[] arguments = pl.words().toArray(new String[0]);
          cmd.execute(arguments);
        } catch (UserInterruptException e) {
          System.exit(0);
        } catch (EndOfFileException e) {
          return;
        }
      }
    }
  }
}
