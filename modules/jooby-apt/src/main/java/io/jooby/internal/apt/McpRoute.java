/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.CodeBlock.*;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;

public class McpRoute extends WebRoute {
  private boolean isMcpTool = false;
  private boolean isMcpPrompt = false;
  private boolean isMcpResource = false;
  private boolean isMcpResourceTemplate = false;
  private boolean isMcpCompletion = false;

  public McpRoute(WebRouter<?> router, ExecutableElement method) {
    super(router, method);
    checkMcpAnnotations();
  }

  private void checkMcpAnnotations() {
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.McpTool")
        != null) {
      this.isMcpTool = true;
    }
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.McpPrompt")
        != null) {
      this.isMcpPrompt = true;
    }
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.McpCompletion")
        != null) {
      this.isMcpCompletion = true;
    }

    var resourceAnno =
        AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.McpResource");
    if (resourceAnno != null) {
      String uri =
          AnnotationSupport.findAnnotationValue(resourceAnno, "value"::equals).stream()
              .findFirst()
              .orElse("");
      if (uri.contains("{") && uri.contains("}")) {
        this.isMcpResourceTemplate = true;
      } else {
        this.isMcpResource = true;
      }
    }
  }

  public boolean isMcpTool() {
    return isMcpTool;
  }

  public boolean isMcpPrompt() {
    return isMcpPrompt;
  }

  public boolean isMcpResource() {
    return isMcpResource;
  }

  public boolean isMcpResourceTemplate() {
    return isMcpResourceTemplate;
  }

  public boolean isMcpCompletion() {
    return isMcpCompletion;
  }

  private String extractAnnotationValue(String annotationName, String attribute) {
    var annotation = AnnotationSupport.findAnnotationByName(method, annotationName);
    if (annotation == null) return "";
    return AnnotationSupport.findAnnotationValue(annotation, attribute::equals).stream()
        .findFirst()
        .orElse("");
  }

  public List<String> generateMcpDefinitionMethod(boolean kt) {
    List<String> buffer = new ArrayList<>();

    if (isMcpTool()) {
      String toolName = extractAnnotationValue("io.jooby.annotation.McpTool", "name");
      if (toolName.isEmpty()) toolName = getMethodName();
      String description = extractAnnotationValue("io.jooby.annotation.McpTool", "description");

      if (kt) {
        buffer.add(
            statement(
                indent(4),
                "private fun ",
                getMethodName(),
                "ToolSpec(mapper: tools.jackson.databind.ObjectMapper, schemaGenerator:"
                    + " com.github.victools.jsonschema.generator.SchemaGenerator):"
                    + " io.modelcontextprotocol.spec.McpSchema.Tool {"));
        buffer.add(statement(indent(6), "val schema = mapper.createObjectNode()"));
        buffer.add(
            statement(indent(6), "schema.put(", string("type"), ", ", string("object"), ")"));
        buffer.add(
            statement(indent(6), "val props = schema.putObject(", string("properties"), ")"));
        buffer.add(statement(indent(6), "val req = schema.putArray(", string("required"), ")"));
      } else {
        buffer.add(
            statement(
                indent(4),
                "private io.modelcontextprotocol.spec.McpSchema.Tool ",
                getMethodName(),
                "ToolSpec(tools.jackson.databind.ObjectMapper mapper,"
                    + " com.github.victools.jsonschema.generator.SchemaGenerator"
                    + " schemaGenerator) {"));
        buffer.add(statement(indent(6), "var schema = mapper.createObjectNode()", semicolon(kt)));
        buffer.add(
            statement(
                indent(6),
                "schema.put(",
                string("type"),
                ", ",
                string("object"),
                ")",
                semicolon(kt)));
        buffer.add(
            statement(
                indent(6),
                "var props = schema.putObject(",
                string("properties"),
                ")",
                semicolon(kt)));
        buffer.add(
            statement(
                indent(6), "var req = schema.putArray(", string("required"), ")", semicolon(kt)));
      }

      for (MvcParameter param : getParameters(false)) {
        String type = param.getType().getRawType().toString();
        if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")
            || type.equals("io.jooby.Context")) continue;

        String mcpName = param.getMcpName();

        if (kt) {
          buffer.add(
              statement(
                  indent(6),
                  "props.set<tools.jackson.databind.JsonNode>(",
                  string(mcpName),
                  ", schemaGenerator.generateSchema(",
                  type,
                  "::class.java))"));
          if (!param.isNullable(kt))
            buffer.add(statement(indent(6), "req.add(", string(mcpName), ")"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "props.set(",
                  string(mcpName),
                  ", schemaGenerator.generateSchema(",
                  type,
                  ".class))",
                  semicolon(kt)));
          if (!param.isNullable(kt))
            buffer.add(statement(indent(6), "req.add(", string(mcpName), ")", semicolon(kt)));
        }
      }

      String returnTypeStr = getReturnType().getRawType().toString();
      boolean isPrimitive =
          returnTypeStr.equals("int")
              || returnTypeStr.equals("long")
              || returnTypeStr.equals("double")
              || returnTypeStr.equals("float")
              || returnTypeStr.equals("boolean")
              || returnTypeStr.equals("byte")
              || returnTypeStr.equals("short")
              || returnTypeStr.equals("char");
      boolean isLangClass = returnTypeStr.startsWith("java.lang.");
      boolean isMcpClass = returnTypeStr.startsWith("io.modelcontextprotocol.spec.McpSchema");

      boolean generateOutputSchema =
          !returnType.isVoid()
              && !getReturnType().is("io.jooby.StatusCode")
              && !isPrimitive
              && !isLangClass
              && !isMcpClass;
      String outputSchemaArg = "null";

      if (generateOutputSchema) {
        outputSchemaArg = getMethodName() + "OutputSchema";
        if (kt) {
          buffer.add(
              statement(
                  indent(6),
                  "val ",
                  outputSchemaArg,
                  "Node = schemaGenerator.generateSchema(",
                  returnTypeStr,
                  "::class.java)"));
          buffer.add(
              statement(
                  indent(6),
                  "val ",
                  outputSchemaArg,
                  " = mapper.convertValue(",
                  outputSchemaArg,
                  "Node, Map::class.java) as Map<String, Any>"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "var ",
                  outputSchemaArg,
                  "Node = schemaGenerator.generateSchema(",
                  returnTypeStr,
                  ".class)",
                  semicolon(kt)));
          buffer.add(
              statement(
                  indent(6),
                  "var ",
                  outputSchemaArg,
                  " = mapper.convertValue(",
                  outputSchemaArg,
                  "Node, java.util.Map.class)",
                  semicolon(kt)));
        }
      }

      if (kt) {
        buffer.add(
            statement(
                indent(6),
                "return io.modelcontextprotocol.spec.McpSchema.Tool(",
                string(toolName),
                ", null, ",
                string(description),
                ", mapper.treeToValue(schema,"
                    + " io.modelcontextprotocol.spec.McpSchema.JsonSchema::class.java), ",
                outputSchemaArg,
                ", null, null)"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return new io.modelcontextprotocol.spec.McpSchema.Tool(",
                string(toolName),
                ", null, ",
                string(description),
                ", mapper.treeToValue(schema,"
                    + " io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), ",
                outputSchemaArg,
                ", null, null)",
                semicolon(kt)));
      }
      buffer.add(statement(indent(4), "}\n"));

    } else if (isMcpPrompt()) {
      String promptName = extractAnnotationValue("io.jooby.annotation.McpPrompt", "name");
      if (promptName.isEmpty()) promptName = getMethodName();
      String description = extractAnnotationValue("io.jooby.annotation.McpPrompt", "description");

      if (kt) {
        buffer.add(
            statement(
                indent(4),
                "private fun ",
                getMethodName(),
                "PromptSpec(): io.modelcontextprotocol.spec.McpSchema.Prompt {"));
        buffer.add(
            statement(
                indent(6),
                "val args ="
                    + " mutableListOf<io.modelcontextprotocol.spec.McpSchema.PromptArgument>()"));
      } else {
        buffer.add(
            statement(
                indent(4),
                "private io.modelcontextprotocol.spec.McpSchema.Prompt ",
                getMethodName(),
                "PromptSpec() {"));
        buffer.add(
            statement(
                indent(6),
                "var args = new"
                    + " java.util.ArrayList<io.modelcontextprotocol.spec.McpSchema.PromptArgument>()",
                semicolon(kt)));
      }

      for (MvcParameter param : getParameters(false)) {
        String type = param.getType().getRawType().toString();
        if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")
            || type.equals("io.jooby.Context")) continue;

        String mcpName = param.getMcpName();
        boolean isRequired = !param.isNullable(kt);

        if (kt) {
          buffer.add(
              statement(
                  indent(6),
                  "args.add(io.modelcontextprotocol.spec.McpSchema.PromptArgument(",
                  string(mcpName),
                  ", null, ",
                  String.valueOf(isRequired),
                  "))"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "args.add(new io.modelcontextprotocol.spec.McpSchema.PromptArgument(",
                  string(mcpName),
                  ", null, ",
                  String.valueOf(isRequired),
                  "))",
                  semicolon(kt)));
        }
      }

      if (kt) {
        buffer.add(
            statement(
                indent(6),
                "return io.modelcontextprotocol.spec.McpSchema.Prompt(",
                string(promptName),
                ", null, ",
                string(description),
                ", args)"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return new io.modelcontextprotocol.spec.McpSchema.Prompt(",
                string(promptName),
                ", null, ",
                string(description),
                ", args)",
                semicolon(kt)));
      }
      buffer.add(statement(indent(4), "}\n"));

    } else if (isMcpResource() || isMcpResourceTemplate()) {
      String uri = extractAnnotationValue("io.jooby.annotation.McpResource", "value");
      String name = extractAnnotationValue("io.jooby.annotation.McpResource", "name");
      if (name.isEmpty()) name = getMethodName();
      String description = extractAnnotationValue("io.jooby.annotation.McpResource", "description");

      boolean isTemplate = isMcpResourceTemplate();
      String specType = isTemplate ? "ResourceTemplate" : "Resource";

      if (kt) {
        buffer.add(
            statement(
                indent(4),
                "private fun ",
                getMethodName(),
                specType,
                "Spec(): io.modelcontextprotocol.spec.McpSchema.",
                specType,
                " {"));
        if (!isTemplate) {
          buffer.add(
              statement(
                  indent(6),
                  "return io.modelcontextprotocol.spec.McpSchema.Resource(",
                  string(uri),
                  ", ",
                  string(name),
                  ", null, ",
                  string(description),
                  ", null, null, null, null)"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "return io.modelcontextprotocol.spec.McpSchema.ResourceTemplate(",
                  string(uri),
                  ", ",
                  string(name),
                  ", null, ",
                  string(description),
                  ", null, null, null)"));
        }
        buffer.add(statement(indent(4), "}\n"));
      } else {
        buffer.add(
            statement(
                indent(4),
                "private io.modelcontextprotocol.spec.McpSchema.",
                specType,
                " ",
                getMethodName(),
                specType,
                "Spec() {"));
        if (!isTemplate) {
          buffer.add(
              statement(
                  indent(6),
                  "return new io.modelcontextprotocol.spec.McpSchema.Resource(",
                  string(uri),
                  ", ",
                  string(name),
                  ", null, ",
                  string(description),
                  ", null, null, null, null)",
                  semicolon(kt)));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "return new io.modelcontextprotocol.spec.McpSchema.ResourceTemplate(",
                  string(uri),
                  ", ",
                  string(name),
                  ", null, ",
                  string(description),
                  ", null, null, null)",
                  semicolon(kt)));
        }
        buffer.add(statement(indent(4), "}\n"));
      }
    }
    return buffer;
  }

  public List<String> generateMcpHandlerMethod(boolean kt) {
    List<String> buffer = new ArrayList<>();

    String reqType = "";
    String resType = "";
    String toMethod = "";

    if (isMcpTool()) {
      reqType = "CallToolRequest";
      resType = "CallToolResult";
      toMethod = "toCallToolResult";
    } else if (isMcpPrompt()) {
      reqType = "GetPromptRequest";
      resType = "GetPromptResult";
      toMethod = "toPromptResult";
    } else if (isMcpResource() || isMcpResourceTemplate()) {
      reqType = "ReadResourceRequest";
      resType = "ReadResourceResult";
      toMethod = "toResourceResult";
    } else {
      return buffer;
    }

    if (kt) {
      buffer.add(
          statement(
              indent(4),
              "private fun ",
              getMethodName(),
              "(exchange: io.modelcontextprotocol.server.McpSyncServerExchange, req:"
                  + " io.modelcontextprotocol.spec.McpSchema.",
              reqType,
              "): io.modelcontextprotocol.spec.McpSchema.",
              resType,
              " {"));
      buffer.add(
          statement(
              indent(6),
              "val ctx ="
                  + " exchange.transportContext().get<io.jooby.Context>(io.jooby.Context::class.java.name)"));
    } else {
      buffer.add(
          statement(
              indent(4),
              "private io.modelcontextprotocol.spec.McpSchema.",
              resType,
              " ",
              getMethodName(),
              "(io.modelcontextprotocol.server.McpSyncServerExchange exchange,"
                  + " io.modelcontextprotocol.spec.McpSchema.",
              reqType,
              " req) {"));
      buffer.add(
          statement(
              indent(6),
              "var ctx = (io.jooby.Context) exchange.transportContext().get(\"CTX\")",
              semicolon(kt)));
    }

    if (isMcpTool() || isMcpPrompt()) {
      if (kt) {
        buffer.add(statement(indent(6), "val args = req.arguments() ?: emptyMap<String, Any>()"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "var args = req.arguments() != null ? req.arguments() :"
                    + " java.util.Collections.<String, Object>emptyMap()",
                semicolon(kt)));
      }
    } else if (isMcpResource() || isMcpResourceTemplate()) {
      String uriTemplate = extractAnnotationValue("io.jooby.annotation.McpResource", "value");
      boolean isTemplate = isMcpResourceTemplate();

      if (isTemplate) {
        if (kt) {
          buffer.add(statement(indent(6), "val uri = req.uri()"));
          buffer.add(
              statement(
                  indent(6),
                  "val manager = io.modelcontextprotocol.util.DefaultMcpUriTemplateManager(",
                  string(uriTemplate),
                  ")"));
          buffer.add(statement(indent(6), "val args = mutableMapOf<String, Any>()"));
          buffer.add(statement(indent(6), "args.putAll(manager.extractVariableValues(uri))"));
        } else {
          buffer.add(statement(indent(6), "var uri = req.uri()", semicolon(kt)));
          buffer.add(
              statement(
                  indent(6),
                  "var manager = new io.modelcontextprotocol.util.DefaultMcpUriTemplateManager(",
                  string(uriTemplate),
                  ")",
                  semicolon(kt)));
          buffer.add(
              statement(
                  indent(6), "var args = new java.util.HashMap<String, Object>()", semicolon(kt)));
          buffer.add(
              statement(
                  indent(6), "args.putAll(manager.extractVariableValues(uri))", semicolon(kt)));
        }
      } else {
        if (kt) {
          buffer.add(statement(indent(6), "val args = emptyMap<String, Any>()"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "var args = java.util.Collections.<String, Object>emptyMap()",
                  semicolon(kt)));
        }
      }
    }

    buffer.add(
        statement(indent(6), kt ? "val " : "var ", "c = this.factory.apply(ctx)", semicolon(kt)));

    List<String> javaParamNames = new ArrayList<>();
    for (MvcParameter param : getParameters(false)) {
      String javaName = param.getName();
      String mcpName = param.getMcpName();
      String type = param.getType().getRawType().toString();
      boolean isNullable = param.isNullable(kt);
      javaParamNames.add(javaName);

      if (type.equals("io.jooby.Context")) {
        buffer.add(statement(indent(6), kt ? "val " : "var ", javaName, " = ctx", semicolon(kt)));
        continue;
      } else if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")) {
        buffer.add(
            statement(indent(6), kt ? "val " : "var ", javaName, " = exchange", semicolon(kt)));
        continue;
      } else if (type.equals("io.modelcontextprotocol.spec.McpSchema." + reqType)) {
        buffer.add(statement(indent(6), kt ? "val " : "var ", javaName, " = req", semicolon(kt)));
        continue;
      }

      if (kt) {
        buffer.add(
            statement(indent(6), "val raw_", javaName, " = args.get(", string(mcpName), ")"));
        if (!isNullable)
          buffer.add(
              statement(
                  indent(6),
                  "if (raw_",
                  javaName,
                  " == null) throw IllegalArgumentException(",
                  string("Missing req param: " + mcpName),
                  ")"));
        buffer.add(
            statement(
                indent(6),
                "val ",
                javaName,
                " = raw_",
                javaName,
                " as ",
                type,
                isNullable ? "?" : ""));
      } else {
        buffer.add(
            statement(
                indent(6),
                "var raw_",
                javaName,
                " = args.get(",
                string(mcpName),
                ")",
                semicolon(kt)));
        if (!isNullable)
          buffer.add(
              statement(
                  indent(6),
                  "if (raw_",
                  javaName,
                  " == null) throw new IllegalArgumentException(",
                  string("Missing req param: " + mcpName),
                  ")",
                  semicolon(kt)));

        if (type.equals("int") || type.equals("java.lang.Integer")) {
          buffer.add(
              statement(
                  indent(6),
                  "var ",
                  javaName,
                  " = ",
                  isNullable ? "(raw_" + javaName + " == null) ? null : " : "",
                  "raw_",
                  javaName,
                  " instanceof Number ? ((Number) raw_",
                  javaName,
                  ").intValue() : Integer.parseInt(raw_",
                  javaName,
                  ".toString())",
                  semicolon(kt)));
        } else if (type.equals("java.lang.String")) {
          buffer.add(
              statement(
                  indent(6),
                  "var ",
                  javaName,
                  " = raw_",
                  javaName,
                  " != null ? raw_",
                  javaName,
                  ".toString() : null",
                  semicolon(kt)));
        } else {
          buffer.add(
              statement(
                  indent(6), "var ", javaName, " = (", type, ") raw_", javaName, semicolon(kt)));
        }
      }
    }

    String methodCall = "c." + getMethodName() + "(" + String.join(", ", javaParamNames) + ")";

    if (getReturnType().isVoid()) {
      buffer.add(statement(indent(6), methodCall, semicolon(kt)));
      if (kt) {
        buffer.add(
            statement(indent(6), "return io.jooby.mcp.McpResult(this.json).", toMethod, "(null)"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return new io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(null)",
                semicolon(kt)));
      }
    } else {
      if (kt) {
        buffer.add(statement(indent(6), "val result = ", methodCall));
        buffer.add(
            statement(
                indent(6), "return io.jooby.mcp.McpResult(this.json).", toMethod, "(result)"));
      } else {
        buffer.add(statement(indent(6), "var result = ", methodCall, semicolon(kt)));
        buffer.add(
            statement(
                indent(6),
                "return new io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(result)",
                semicolon(kt)));
      }
    }
    buffer.add(statement(indent(4), "}\n"));

    return buffer;
  }
}
