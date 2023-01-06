/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class Issue2303 {

  @Test
  public void shouldLoadAbsoluteFile() throws IOException {
    String crt = file("ssl", "test.crt");
    String key = file("ssl", "test.key");
    SslOptions ssl = SslOptions.x509(crt, key);
    assertNotNull(ssl);
    assertNotNull(ssl.getCert());
    assertNotNull(ssl.getPrivateKey());
  }

  private String file(String... path) {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (Files.exists(basedir.resolve("jooby"))) {
      // maven vs IDE
      basedir = basedir.resolve("jooby");
    }
    Path result = basedir.resolve("src").resolve("test").resolve("resources");
    for (String segment : path) {
      result = result.resolve(segment);
    }
    result = result.normalize().toAbsolutePath();
    assertTrue(Files.exists(result), result.toString());
    return result.toString();
  }
}
