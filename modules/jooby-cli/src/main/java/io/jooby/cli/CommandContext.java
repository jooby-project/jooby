/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.jline.reader.LineReader;

import java.io.PrintWriter;

public class CommandContext {

  public final LineReader reader;

  public final Handlebars templates;

  public final PrintWriter out;

  public CommandContext(LineReader reader) {
    this.reader = reader;
    this.out = reader.getTerminal().writer();
    TemplateLoader loader = new ClassPathTemplateLoader("/cli");
    this.templates = new Handlebars(loader);
    this.templates.setPrettyPrint(true);
  }

  public void exit(int code) {
    System.exit(code);
  }
}
