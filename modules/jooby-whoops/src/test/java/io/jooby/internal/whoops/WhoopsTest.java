/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.whoops;

import org.junit.jupiter.api.Test;

import io.pebbletemplates.pebble.PebbleEngine;

public class WhoopsTest {

  @Test
  public void shouldParseTemplates() {
    PebbleEngine engine = Whoops.engine();
    String[] templates = {
      "env_details",
      "frame_code",
      "frame_list",
      "frames_container",
      "frames_description",
      "header",
      "header_outer",
      "layout",
      "panel_details",
      "panel_details_outer",
      "panel_left",
      "panel_left_outer"
    };
    for (String template : templates) {
      engine.getTemplate(template);
    }
  }
}
