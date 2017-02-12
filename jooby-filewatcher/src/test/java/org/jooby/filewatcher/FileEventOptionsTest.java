package org.jooby.filewatcher;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

import static org.junit.Assert.*;

public class FileEventOptionsTest {

  public static class MyHandler implements FileEventHandler {

    @Override
    public void handle(final Kind<Path> kind, final Path path) throws IOException {
    }
  }

  @Test
  public void defaults() throws IOException {
    Path source = Paths.get(".");
    MyHandler handler = new MyHandler();
    FileEventOptions options = new FileEventOptions(source, MyHandler.class);
    assertEquals(handler, options.handler(type -> handler));
    assertEquals(source, options.path());
    assertEquals("**/*", options.filter().toString());
    assertEquals(true, options.filter().matches(null));
    assertArrayEquals(new Object[]{StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY},
        options.kinds());
    assertEquals("HIGH", options.modifier().name());
    assertEquals(true, options.recursive());
    assertEquals(
        ". {kinds: [ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY], filter: **/*, recursive: true, modifier: HIGH}",
        options.toString());
  }

  @Test
  public void withInstance() throws IOException {
    Path source = Paths.get(".");
    MyHandler handler = new MyHandler();
    FileEventOptions options = new FileEventOptions(source, handler);
    assertEquals(handler, options.handler(type -> handler));
    assertEquals(source, options.path());
    assertEquals("**/*", options.filter().toString());
    assertEquals(true, options.filter().matches(null));
    assertArrayEquals(new Object[]{StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY},
        options.kinds());
    assertEquals("HIGH", options.modifier().name());
    assertEquals(true, options.recursive());
    assertEquals(
        ". {kinds: [ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY], filter: **/*, recursive: true, modifier: HIGH}",
        options.toString());
  }

  @Test
  public void mkdir() throws IOException {
    Path source = Paths.get("target/" + FileEventOptions.class.getSimpleName());
    new FileEventOptions(source, MyHandler.class);
    assertTrue(Files.exists(source));
  }

  @Test
  public void filter() throws IOException {
    assertEquals("[**/*.java]", new FileEventOptions(Paths.get("."), MyHandler.class)
        .includes("**/*.java")
        .filter()
        .toString());

    assertEquals("[**/*.java, **/*.kt]", new FileEventOptions(Paths.get("."), MyHandler.class)
        .includes("**/*.java")
        .includes("**/*.kt")
        .filter()
        .toString());
  }

  @Test
  public void modifier() throws IOException {
    assertEquals("LOW", new FileEventOptions(Paths.get("."), MyHandler.class)
        .modifier(() -> "LOW")
        .modifier()
        .name());
  }

  @Test
  public void recursive() throws IOException {
    assertEquals(false, new FileEventOptions(Paths.get("."), MyHandler.class)
        .recursive(false)
        .recursive());
  }

  @Test
  public void kind() throws IOException {
    assertArrayEquals(new Object[]{StandardWatchEventKinds.ENTRY_CREATE},
        new FileEventOptions(Paths.get("."), MyHandler.class)
            .kind(StandardWatchEventKinds.ENTRY_CREATE)
            .kinds());

    assertArrayEquals(new Object[]{StandardWatchEventKinds.ENTRY_MODIFY},
        new FileEventOptions(Paths.get("."), MyHandler.class)
            .kind(StandardWatchEventKinds.ENTRY_MODIFY)
            .kinds());

    assertArrayEquals(new Object[]{StandardWatchEventKinds.ENTRY_DELETE},
        new FileEventOptions(Paths.get("."), MyHandler.class)
            .kind(StandardWatchEventKinds.ENTRY_DELETE)
            .kinds());

    assertArrayEquals(
        new Object[]{StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE},
        new FileEventOptions(Paths.get("."), MyHandler.class)
            .kind(StandardWatchEventKinds.ENTRY_MODIFY)
            .kind(StandardWatchEventKinds.ENTRY_CREATE)
            .kinds());
  }
}
