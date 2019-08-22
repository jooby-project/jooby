/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

import io.jooby.cli.Cli;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

  @Override public String[] getVersion() {
    return new String[] {Cli.version};
  }
}
