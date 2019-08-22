/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import io.jooby.internal.cli.CommandContextImpl;
import io.jooby.internal.cli.JLineCompleter;
import io.jooby.internal.cli.VersionProvider;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import picocli.CommandLine;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application console.
 *
 * Usage:
 * <pre>{@code
 * jooby> --help
 * Usage: jooby [-hV] [COMMAND]
 *   -h, --help      Show this help message and exit.
 *   -V, --version   Print version information and exit.
 * Commands:
 *   create  Creates a new application
 *   exit    Exit console
 * }</pre>
 *
 * @since 2.0.6
 */
@CommandLine.Command(
    name = "jooby",
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true,
    version = "Print version information"
)
public class Cli extends Cmd {
  public static String version;

  /** Command line specification.  */
  private @CommandLine.Spec CommandLine.Model.CommandSpec spec;

  /** Unmatched command line arguments. */
  private @CommandLine.Unmatched List<String> args;

  @Override public void run(@Nonnull Context ctx) {
    List<String> args = this.args.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(it -> it.length() > 0)
        .collect(Collectors.toList());
    if (args.size() > 0) {
      String arg = args.get(0);
      if ("-h".equals(arg) || "--help".equals(arg)) {
        ctx.println(spec.commandLine().getUsageMessage());
      } else if ("-V".equalsIgnoreCase(arg) || "--version".equals(arg)) {
        ctx.println(ctx.getVersion());
      } else {
        ctx.println("Unknown command or option(s): " + args.stream().collect(Collectors.joining(" ")));
      }
    }
  }

  /**
   * Start a jooby console or execute given arguments and exits.
   *
   * @param args Command line arguments.
   * @throws IOException If something goes wrong.
   */
  public static void main(String[] args) throws IOException {
    version = checkVersion();
    // set up the completion
    Cli jooby = new Cli();
    CommandLine cmd = new CommandLine(jooby)
        .addSubcommand(new CreateCmd())
        .addSubcommand(new ExitCmd())
        .addSubcommand(new SetCmd());

    Terminal terminal = TerminalBuilder.builder().build();
    LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(new JLineCompleter(cmd.getCommandSpec()))
        .parser(new DefaultParser())
        .build();

    CommandContextImpl context = new CommandContextImpl(reader, version);
    jooby.setContext(context);
    cmd.getSubcommands().values().stream()
        .map(CommandLine::getCommand)
        .filter(Cmd.class::isInstance)
        .map(Cmd.class::cast)
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

  private static String checkVersion() {
    try {
      URL url = URI
          .create("http://search.maven.org/solrsearch/select?q=+g:io.jooby+a:jooby&start=0&rows=1")
          .toURL();
      URLConnection connection = url.openConnection();
      try(InputStream in = connection.getInputStream()) {
        JSONObject json = new JSONObject(new JSONTokener(in));
        JSONObject response = json.getJSONObject("response");
        JSONArray docs = response.getJSONArray("docs");
        JSONObject jooby = docs.getJSONObject(0);
        return jooby.getString("latestVersion");
      }
    } catch (Exception x) {
      return Optional.ofNullable(VersionProvider.class.getPackage())
          .map(Package::getImplementationVersion)
          .filter(Objects::nonNull)
          .orElse("2.0.5");
    }
  }
}
