package filewatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;

import org.jooby.filewatcher.FileEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFileEventHandler implements FileEventHandler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void handle(final Kind<Path> kind, final Path path) throws IOException {
    log.info("{}({}) exists? {}", path, kind, Files.exists(path));
  }

}
