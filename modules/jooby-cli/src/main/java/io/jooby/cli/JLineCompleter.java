/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import org.jline.reader.LineReader;
import org.jline.reader.Completer;
import org.jline.reader.Candidate;
import org.jline.reader.ParsedLine;
import picocli.AutoComplete;
import picocli.CommandLine.Model.CommandSpec;

import java.util.List;
import java.util.ArrayList;
import java.lang.CharSequence;

class JLineCompleter implements Completer {
  private final CommandSpec spec;

  public JLineCompleter(CommandSpec spec) {
    this.spec = spec;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    List<CharSequence> completion = new ArrayList<>();
    AutoComplete.complete(spec,
        line.words().toArray(new String[line.words().size()]),
        line.wordIndex(),
        0,
        line.cursor(),
        completion);
    for (CharSequence c : completion) {
      candidates.add(new Candidate(c.toString()));
    }
  }
}
