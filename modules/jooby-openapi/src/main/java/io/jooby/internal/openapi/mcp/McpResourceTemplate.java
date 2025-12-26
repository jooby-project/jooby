/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.mcp;

import io.swagger.v3.oas.models.media.Schema;

public record McpResourceTemplate(
    String uriTemplate, String name, String description, String mimeType, Schema<?> parameters) {}
