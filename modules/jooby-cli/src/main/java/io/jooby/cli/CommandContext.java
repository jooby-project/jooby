package io.jooby.cli;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public interface CommandContext {
  void exit(int code);

  void writeTemplate(String template, Object model, Path file) throws IOException;

  void writeTemplate(String template, Object model, Writer writer) throws IOException;

  void copyResource(String source, Path dest) throws IOException;

  void copyResource(String source, Path dest, Set<PosixFilePermission> permissions)
      throws IOException;

  String readLine(String prompt);

  void println(String message);
}
