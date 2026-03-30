/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

/**
 * Contextual information about an MCP operation being invoked.
 *
 * @param id The standard MCP identifier (e.g., "tools/add_numbers").
 * @param className The fully qualified name of the Java/Kotlin class hosting the method.
 * @param methodName The name of the Java/Kotlin method being executed.
 * @author edgar
 * @since 4.2.0
 */
public record McpOperation(String id, String className, String methodName) {}
