/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp.jackson3;

import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;

public class McpJackson3Module implements Extension {
  @Override
  public void install(Jooby application) throws Exception {
    var services = application.getServices();
    var jsonMapper = services.require(JsonMapper.class);
    var mcpJsonMapper = new JacksonMcpJsonMapper(jsonMapper);
    services.put(McpJsonMapper.class, mcpJsonMapper);

    // schema generator
    var configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    var schemaGenerator = new SchemaGenerator(configBuilder.build());
    services.put(SchemaGenerator.class, schemaGenerator);
  }
}
