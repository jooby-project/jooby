/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import io.avaje.jsonb.Json;

@Json
public record Metadata(String releaseDate) {}
