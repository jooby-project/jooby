package io.jooby;

import javax.net.ssl.SSLContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public interface SSLContextProvider {

  boolean supports(String type);

  SSLContext create(ClassLoader loader, SSLOptions options);

  static InputStream loadFile(ClassLoader loader, String path) throws IOException {
    InputStream in = loadFileFromFileSystem(path);
    if (in == null) {
      in = loadFileFromClasspath(loader, path);
      if (in == null) {
        throw new FileNotFoundException(path);
      }
    }
    return in;
  }

  static InputStream loadFileFromClasspath(ClassLoader loader, String path) {
    return path.startsWith("/")
        ? loader.getResourceAsStream(path.substring(1))
        : loader.getResourceAsStream(path);
  }

  static InputStream loadFileFromFileSystem(String path) throws IOException {
    Path file = Stream.of(Paths.get(path), Paths.get(System.getProperty("user.dir"), path))
        .filter(Files::exists)
        .findFirst()
        .orElse(null);
    if (file != null) {
      return Files.newInputStream(file);
    }
    return null;
  }
}
