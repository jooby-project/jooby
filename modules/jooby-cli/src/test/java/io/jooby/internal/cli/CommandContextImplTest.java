package io.jooby.internal.cli;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandContextImplTest {

  @Test
  public void shouldLoadDependencyFile() throws IOException {
    PrintWriter writer = mock(PrintWriter.class);

    Terminal terminal = mock(Terminal.class);
    when(terminal.writer()).thenReturn(writer);

    LineReader reader = mock(LineReader.class);
    when(reader.getTerminal()).thenReturn(terminal);

    CommandContextImpl ctx = new CommandContextImpl(reader, "2.8.5");

    Map<String, String> dependencyMap = ctx.getDependencyMap();
    assertNotNull(dependencyMap);
    assertNotNull(dependencyMap.get("mavenTilesPluginVersion"));
  }

  @Test
  public void shouldCopyGradleJar() throws IOException {
    PrintWriter writer = mock(PrintWriter.class);

    Terminal terminal = mock(Terminal.class);
    when(terminal.writer()).thenReturn(writer);

    LineReader reader = mock(LineReader.class);
    when(reader.getTerminal()).thenReturn(terminal);

    CommandContextImpl ctx = new CommandContextImpl(reader, "2.8.5");
    Path dest = Paths.get(System.getProperty("java.io.tmpdir"), "gradle-wrapper.jar");
    ctx.copyResource("/cli/gradle/gradle/wrapper/gradle-wrapper.jar", dest);
    assertTrue(Files.exists(dest));
    Files.delete(dest);
  }
}
