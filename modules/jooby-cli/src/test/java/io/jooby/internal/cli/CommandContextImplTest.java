package io.jooby.internal.cli;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
