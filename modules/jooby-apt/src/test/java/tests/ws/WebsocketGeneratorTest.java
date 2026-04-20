/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.ws;

import io.jooby.apt.ProcessorRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebsocketGeneratorTest {

  @Test
  public void chatWebsocketMatchesGeneratedSource() throws Exception {
    var expected = new String(
        getClass()
            .getResourceAsStream("/tests/ws/ChatWebsocketWs_expected.java")
            .readAllBytes()
    );

    new ProcessorRunner(new ChatWebsocket())
        .withWsCode(source -> assertThat(normalize(source))
            .isEqualTo(normalize(expected))
        );
  }

  private static String normalize(String source) {
    return source.replace("\r\n", "\n").replace('\r', '\n')
        .stripTrailing();
  }
}
