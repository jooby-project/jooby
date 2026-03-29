/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import static io.jooby.internal.apt.CodeBlock.*;
import static io.jooby.internal.apt.CodeBlock.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

import io.jooby.javadoc.JavaDocNode;
import io.jooby.javadoc.MethodDoc;

public class McpRoute extends WebRoute<McpRouter> {
  private boolean isMcpTool = false;
  private boolean isMcpPrompt = false;
  private boolean isMcpResource = false;
  private boolean isMcpResourceTemplate = false;
  private boolean isMcpCompletion = false;

  public McpRoute(McpRouter router, ExecutableElement method) {
    super(router, method);
    checkMcpAnnotations();
  }

  private void checkMcpAnnotations() {
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.mcp.McpTool")
        != null) {
      this.isMcpTool = true;
    }
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.mcp.McpPrompt")
        != null) {
      this.isMcpPrompt = true;
    }
    if (AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.mcp.McpCompletion")
        != null) {
      this.isMcpCompletion = true;
    }

    var resourceAnno =
        AnnotationSupport.findAnnotationByName(this.method, "io.jooby.annotation.mcp.McpResource");
    if (resourceAnno != null) {
      String uri =
          AnnotationSupport.findAnnotationValue(resourceAnno, "uri"::equals).stream()
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
    if (annotation == null) return null;
    return AnnotationSupport.findAnnotationValue(annotation, attribute::equals).stream()
        .findFirst()
        .filter(it -> !it.isEmpty())
        .orElse(null);
  }

  public List<String> generateMcpDefinitionMethod(boolean kt) {
    if (isMcpTool()) {
      return generateToolDefinition(kt);
    } else if (isMcpPrompt()) {
      return generatePromptDefinition(kt);
    } else if (isMcpResource() || isMcpResourceTemplate()) {
      return generateResourceDefinition(kt);
    }
    // unreachable
    return List.of();
  }

  private List<String> generateResourceDefinition(boolean kt) {
    List<String> buffer = new ArrayList<>();
    var method = getMethodDoc(kt);
    var methodSummary = method.map(JavaDocNode::getSummary).orElse(null);
    var methodDescription = method.map(JavaDocNode::getDescription).orElse(null);

    var uri = extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "uri");
    var name =
        Optional.ofNullable(extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "name"))
            .orElse(getMethodName());

    var title =
        Optional.ofNullable(extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "title"))
            .orElse(methodSummary);
    var description =
        Optional.ofNullable(
                extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "description"))
            .orElse(methodDescription);
    var mimeType = extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "mimeType");
    var sizeStr = extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "size");

    // Prepare standard arguments safely
    var titleArg = string(title);
    var descriptionArg = string(description);
    var mimeTypeArg =
        mimeType == null
            ? of(
                "io.jooby.MediaType.byFileExtension(",
                string(uri),
                ", ",
                string("text/plain"),
                ").getValue()")
            : string(mimeType);
    String sizeArg = (sizeStr == null || sizeStr.equals("-1")) ? "null" : sizeStr + "L";

    // --- NESTED ANNOTATION EXTRACTION ---
    String annotationsArg = "null";
    var annotation = parseResourceAnnotation();

    var isTemplate = isMcpResourceTemplate();
    var specType = isTemplate ? "ResourceTemplate" : "Resource";

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

      // Build the Kotlin ResourceAnnotations object if present
      if (annotation != null) {
        buffer.add(statement(indent(6), "val audience = ", "listOf(", annotation.audience, ""));
        buffer.add(
            statement(
                indent(6),
                "val annotations = io.modelcontextprotocol.spec.McpSchema.Annotations(audience, ",
                annotation.priority,
                ", ",
                annotation.lastModified,
                ")"));
      }

      if (!isTemplate) {
        buffer.add(
            statement(
                indent(6),
                "return io.modelcontextprotocol.spec.McpSchema.Resource(",
                string(uri),
                ", ",
                string(name),
                ", ",
                titleArg,
                ", ",
                descriptionArg,
                ", ",
                mimeTypeArg,
                ", ",
                sizeArg,
                ", ",
                annotationsArg,
                ", null)"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return io.modelcontextprotocol.spec.McpSchema.ResourceTemplate(",
                string(uri),
                ", ",
                string(name),
                ", ",
                titleArg,
                ", ",
                descriptionArg,
                ", ",
                mimeTypeArg,
                ", ",
                annotationsArg,
                ", null)"));
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

      // Build the Java ResourceAnnotations object if present
      if (annotation != null) {
        annotationsArg = "annotations";

        buffer.add(
            statement(
                indent(6),
                "var audience = ",
                "java.util.List.of(",
                annotation.audience,
                ")",
                semicolon(kt)));
        buffer.add(
            statement(
                indent(6),
                "var annotations = new"
                    + " io.modelcontextprotocol.spec.McpSchema.Annotations(audience, ",
                annotation.priority,
                "D, ",
                annotation.lastModified,
                ")",
                semicolon(kt)));
      }

      if (!isTemplate) {
        buffer.add(
            statement(
                indent(6),
                "return new io.modelcontextprotocol.spec.McpSchema.Resource(",
                string(uri),
                ", ",
                string(name),
                ", ",
                titleArg,
                ", ",
                descriptionArg,
                ", ",
                mimeTypeArg,
                ", ",
                sizeArg,
                ", ",
                annotationsArg,
                ", null)",
                semicolon(kt)));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return new io.modelcontextprotocol.spec.McpSchema.ResourceTemplate(",
                string(uri),
                ", ",
                string(name),
                ", ",
                titleArg,
                ", ",
                descriptionArg,
                ", ",
                mimeTypeArg,
                ", ",
                annotationsArg,
                ", null)",
                semicolon(kt)));
      }
      buffer.add(statement(indent(4), "}\n"));
    }
    return buffer;
  }

  private List<String> generatePromptDefinition(boolean kt) {
    List<String> buffer = new ArrayList<>();
    var method = getMethodDoc(kt);
    var methodSummary = method.map(JavaDocNode::getSummary).orElse(null);
    var methodDescription = method.map(JavaDocNode::getDescription).orElse(null);

    String promptName =
        Optional.ofNullable(extractAnnotationValue("io.jooby.annotation.mcp.McpPrompt", "name"))
            .orElse(getMethodName());

    String title =
        Optional.ofNullable(extractAnnotationValue("io.jooby.annotation.mcp.McpPrompt", "title"))
            .orElse(methodSummary);
    String description =
        Optional.ofNullable(
                extractAnnotationValue("io.jooby.annotation.mcp.McpPrompt", "description"))
            .orElse(methodDescription);

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

    for (var param : getParameters(true)) {
      var type = param.getType().getRawType().toString();
      if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")
          || type.equals("io.modelcontextprotocol.common.McpTransportContext")
          || type.equals("io.jooby.Context")) continue;

      var mcpName = param.getMcpName();
      var isRequired = !param.isNullable(kt);

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
              ", ",
              string(title),
              ", ",
              string(description),
              ", args)"));
    } else {
      buffer.add(
          statement(
              indent(6),
              "return new io.modelcontextprotocol.spec.McpSchema.Prompt(",
              string(promptName),
              ", ",
              string(title),
              ", ",
              string(description),
              ", args)",
              semicolon(kt)));
    }
    buffer.add(statement(indent(4), "}\n"));
    return buffer;
  }

  private List<String> generateToolDefinition(boolean kt) {
    var buffer = new ArrayList<String>();
    var method = getMethodDoc(kt);
    var methodSummary = method.map(JavaDocNode::getSummary).orElse(null);
    var methodDescription = method.map(JavaDocNode::getDescription).orElse(null);
    var methodSummaryAndDescription = method.map(JavaDocNode::getFullDescription).orElse(null);
    String toolName =
        Optional.ofNullable(extractAnnotationValue("io.jooby.annotation.mcp.McpTool", "name"))
            .orElse(getMethodName());

    // Extract the new title attribute
    String title = extractAnnotationValue("io.jooby.annotation.mcp.McpTool", "title");
    var titleArg = string(Optional.ofNullable(title).orElse(methodSummary));

    String description =
        Optional.ofNullable(
                extractAnnotationValue("io.jooby.annotation.mcp.McpTool", "description"))
            .orElse(methodDescription);

    if (kt) {
      buffer.add(
          statement(
              indent(4),
              "private fun ",
              getMethodName(),
              "ToolSpec(schemaGenerator:"
                  + " com.github.victools.jsonschema.generator.SchemaGenerator):"
                  + " io.modelcontextprotocol.spec.McpSchema.Tool {"));
      buffer.add(statement(indent(6), "val schema = java.util.LinkedHashMap<String, Any>()"));
      buffer.add(statement(indent(6), "schema.put(", string("type"), ", ", string("object"), ")"));
      buffer.add(statement(indent(6), "val props = java.util.LinkedHashMap<String, Any>()"));
      buffer.add(statement(indent(6), "schema.put(", string("properties"), ", props)"));
      buffer.add(statement(indent(6), "val req = java.util.ArrayList<String>()"));
      buffer.add(statement(indent(6), "schema.put(", string("required"), ", req)"));
    } else {
      buffer.add(
          statement(
              indent(4),
              "private io.modelcontextprotocol.spec.McpSchema.Tool ",
              getMethodName(),
              "ToolSpec(com.github.victools.jsonschema.generator.SchemaGenerator"
                  + " schemaGenerator) {"));
      buffer.add(
          statement(
              indent(6),
              "var schema = new java.util.LinkedHashMap<String, Object>()",
              semicolon(kt)));
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
              "var props = new java.util.LinkedHashMap<String, Object>()",
              semicolon(kt)));
      buffer.add(
          statement(indent(6), "schema.put(", string("properties"), ", props)", semicolon(kt)));
      buffer.add(
          statement(indent(6), "var req = new java.util.ArrayList<String>()", semicolon(kt)));
      buffer.add(statement(indent(6), "schema.put(", string("required"), ", req)", semicolon(kt)));
    }

    // --- PARAMETER SCHEMA GENERATION ---
    for (var param : getParameters(true)) {
      var type = param.getType().getRawType().toString();
      if (type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")
          || type.equals("io.modelcontextprotocol.common.McpTransportContext")
          || type.equals("io.jooby.Context")) continue;

      var mcpName = param.getMcpName();
      var paramDescription = param.getMcpDescription();
      if (paramDescription == null) {
        paramDescription = method.map(it -> it.getParameterDoc(param.getName())).orElse("");
      }

      if (kt) {
        buffer.add(
            statement(
                indent(6),
                "val schema_",
                mcpName,
                " = schemaGenerator.generateSchema(",
                type,
                "::class.java)"));

        if (!paramDescription.isEmpty()) {
          buffer.add(
              statement(
                  indent(6),
                  "schema_",
                  mcpName,
                  ".put(",
                  string("description"),
                  ", ",
                  string(paramDescription),
                  ")"));
        }

        // Switched from .set() to .put() for standard Map
        buffer.add(statement(indent(6), "props.put(", string(mcpName), ", schema_", mcpName, ")"));

        if (!param.isNullable(kt)) {
          buffer.add(statement(indent(6), "req.add(", string(mcpName), ")"));
        }
      } else {
        buffer.add(
            statement(
                indent(6),
                "var schema_",
                mcpName,
                " = schemaGenerator.generateSchema(",
                type,
                ".class)",
                semicolon(kt)));

        if (!paramDescription.isEmpty()) {
          buffer.add(
              statement(
                  indent(6),
                  "schema_",
                  mcpName,
                  ".put(",
                  string("description"),
                  ", ",
                  string(paramDescription),
                  ")",
                  semicolon(kt)));
        }

        // Switched from .set() to .put() for standard Map
        buffer.add(
            statement(
                indent(6),
                "props.put(",
                string(mcpName),
                ", schema_",
                mcpName,
                ")",
                semicolon(kt)));

        if (!param.isNullable(kt)) {
          buffer.add(statement(indent(6), "req.add(", string(mcpName), ")", semicolon(kt)));
        }
      }
    }

    // --- OUTPUT SCHEMA GENERATION ---
    String returnTypeStr = getReturnType().getRawType().toString();
    boolean generateOutputSchema = hasOutputSchema();
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
        // Use this.json to convert the output schema
        buffer.add(
            statement(
                indent(6),
                "val ",
                outputSchemaArg,
                " = this.json.convertValue(",
                outputSchemaArg,
                "Node, java.util.Map::class.java) as java.util.Map<String, Any>"));
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
        // Use this.json to convert the output schema
        buffer.add(
            statement(
                indent(6),
                "var ",
                outputSchemaArg,
                " = this.json.convertValue(",
                outputSchemaArg,
                "Node, java.util.Map.class)",
                semicolon(kt)));
      }
    }

    // --- NESTED ANNOTATION EXTRACTION ---
    String annotationsArg = "null";
    var toolAnnotation = parseToolAnnotation();

    if (kt) {
      if (toolAnnotation != null) {
        annotationsArg = "annotations";
        buffer.add(
            statement(
                indent(6),
                "val annotations = io.modelcontextprotocol.spec.McpSchema.ToolAnnotations(",
                methodSummaryAndDescription.isEmpty()
                    ? "null"
                    : string(methodSummaryAndDescription),
                ", ",
                toolAnnotation.readOnlyHint(),
                ", ",
                toolAnnotation.destructiveHint(),
                ", ",
                toolAnnotation.idempotentHint(),
                ", ",
                toolAnnotation.openWorldHint(),
                ", null)"));
      }

      buffer.add(
          statement(
              indent(6),
              "return io.modelcontextprotocol.spec.McpSchema.Tool(",
              string(toolName),
              ", ",
              titleArg,
              ", ",
              string(description),
              // Use this.json to convert the main schema map into JsonSchema
              ", this.json.convertValue(schema,"
                  + " io.modelcontextprotocol.spec.McpSchema.JsonSchema::class.java), ",
              outputSchemaArg,
              ", ",
              annotationsArg,
              ", null)"));
    } else {
      if (toolAnnotation != null) {
        annotationsArg = "annotations";
        buffer.add(
            statement(
                indent(6),
                "var annotations = new io.modelcontextprotocol.spec.McpSchema.ToolAnnotations(",
                methodSummaryAndDescription.isEmpty()
                    ? "null"
                    : string(methodSummaryAndDescription),
                ", ",
                toolAnnotation.readOnlyHint(),
                ", ",
                toolAnnotation.destructiveHint(),
                ", ",
                toolAnnotation.idempotentHint(),
                ", ",
                toolAnnotation.openWorldHint(),
                ", null)",
                semicolon(kt)));
      }

      buffer.add(
          statement(
              indent(6),
              "return new io.modelcontextprotocol.spec.McpSchema.Tool(",
              string(toolName),
              ", ",
              titleArg,
              ", ",
              string(description),
              // Use this.json to convert the main schema map into JsonSchema
              ", this.json.convertValue(schema,"
                  + " io.modelcontextprotocol.spec.McpSchema.JsonSchema.class), ",
              outputSchemaArg,
              ", ",
              annotationsArg,
              ", null)",
              semicolon(kt)));
    }
    buffer.add(statement(indent(4), "}\n"));
    return buffer;
  }

  private Optional<MethodDoc> getMethodDoc(boolean kt) {
    return router.getMethodDoc(getMethodName(), getRawParameterTypes(false, kt, true));
  }

  public List<String> generateMcpHandlerMethod(boolean kt) {
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
      return List.of();
    }

    List<String> buffer = new ArrayList<>();
    String handlerName = getMethodName();

    if (kt) {
      buffer.add(
          statement(
              indent(4),
              "private fun ",
              handlerName,
              "(exchange: io.modelcontextprotocol.server.McpSyncServerExchange?, transportContext:"
                  + " io.modelcontextprotocol.common.McpTransportContext?, req:"
                  + " io.modelcontextprotocol.spec.McpSchema.",
              reqType,
              "): io.modelcontextprotocol.spec.McpSchema.",
              resType,
              " {"));
      buffer.add(
          statement(
              indent(6),
              "val ctx ="
                  + " exchange?.transportContext()?.get<io.jooby.Context>(io.jooby.Context::class.java.name)"
                  + " ?: transportContext?.get<io.jooby.Context>(io.jooby.Context::class.java.name)"));
    } else {
      buffer.add(
          statement(
              indent(4),
              "private io.modelcontextprotocol.spec.McpSchema.",
              resType,
              " ",
              handlerName,
              "(io.modelcontextprotocol.server.McpSyncServerExchange exchange,"
                  + " io.modelcontextprotocol.common.McpTransportContext transportContext,"
                  + " io.modelcontextprotocol.spec.McpSchema.",
              reqType,
              " req) {"));
      buffer.add(
          statement(
              indent(6),
              "var ctx = exchange != null ? (io.jooby.Context)"
                  + " exchange.transportContext().get(\"CTX\") : (transportContext != null ?"
                  + " (io.jooby.Context) transportContext.get(\"CTX\") : null)",
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
      String uriTemplate = extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "uri");
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
    for (var param : getParameters(true)) {
      var javaName = param.getName();
      var mcpName = param.getMcpName();
      var type = param.getType().getRawType().toString();
      var isNullable = param.isNullable(kt);
      javaParamNames.add(javaName);

      if (type.equals("io.jooby.Context")
          || type.equals("io.modelcontextprotocol.server.McpSyncServerExchange")) {
        continue;
      }
      if (type.equals("io.modelcontextprotocol.common.McpTransportContext")) {
        if (kt) {
          buffer.add(
              statement(
                  indent(6),
                  "val ",
                  javaName,
                  " = exchange?.transportContext() ?: transportContext"));
        } else {
          buffer.add(
              statement(
                  indent(6),
                  "var ",
                  javaName,
                  " = exchange != null ? exchange.transportContext() : transportContext",
                  semicolon(kt)));
        }
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

    var methodCall = "c." + getMethodName() + "(" + String.join(", ", javaParamNames) + ")";

    // Prefix for Resources: "req.uri(), "
    String toMethodPrefix = (isMcpResource() || isMcpResourceTemplate()) ? "req.uri(), " : "";

    // Suffix for Tools: ", true" or ", false"
    String toMethodSuffix = isMcpTool() ? ", " + hasOutputSchema() : "";

    if (getReturnType().isVoid()) {
      buffer.add(statement(indent(6), methodCall, semicolon(kt)));
      if (kt) {
        buffer.add(
            statement(
                indent(6),
                "return io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(",
                toMethodPrefix,
                "null",
                toMethodSuffix,
                ")"));
      } else {
        buffer.add(
            statement(
                indent(6),
                "return new io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(",
                toMethodPrefix,
                "null",
                toMethodSuffix,
                ")",
                semicolon(kt)));
      }
    } else {
      if (kt) {
        buffer.add(statement(indent(6), "val result = ", methodCall));
        buffer.add(
            statement(
                indent(6),
                "return io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(",
                toMethodPrefix,
                "result",
                toMethodSuffix,
                ")"));
      } else {
        buffer.add(statement(indent(6), "var result = ", methodCall, semicolon(kt)));
        buffer.add(
            statement(
                indent(6),
                "return new io.jooby.mcp.McpResult(this.json).",
                toMethod,
                "(",
                toMethodPrefix,
                "result",
                toMethodSuffix,
                ")",
                semicolon(kt)));
      }
    }
    buffer.add(statement(indent(4), "}", System.lineSeparator()));

    return buffer;
  }

  private boolean hasOutputSchema() {
    var returnTypeStr = getReturnType().getRawType().toString();
    var isPrimitive =
        returnTypeStr.equals("int")
            || returnTypeStr.equals("long")
            || returnTypeStr.equals("double")
            || returnTypeStr.equals("float")
            || returnTypeStr.equals("boolean")
            || returnTypeStr.equals("byte")
            || returnTypeStr.equals("short")
            || returnTypeStr.equals("char");
    var isLangClass = returnTypeStr.startsWith("java.lang.");
    var isMcpClass = returnTypeStr.startsWith("io.modelcontextprotocol.spec.McpSchema");

    return !getReturnType().isVoid()
        && !getReturnType().is("io.jooby.StatusCode")
        && !isPrimitive
        && !isLangClass
        && !isMcpClass;
  }

  private McpAnnotation parseResourceAnnotation() {
    String rawAnnotations =
        extractAnnotationValue("io.jooby.annotation.mcp.McpResource", "annotations");

    boolean hasAnnotations = rawAnnotations != null && rawAnnotations.contains("priority=");

    if (!hasAnnotations) {
      return null;
    }

    var audienceList = new ArrayList<String>();
    var annotationMap =
        Map.of(
            "USER",
            "io.modelcontextprotocol.spec.McpSchema.Role.USER",
            "ASSISTANT",
            "io.modelcontextprotocol.spec.McpSchema.Role.ASSISTANT");
    for (var entry : annotationMap.entrySet()) {
      if (rawAnnotations.contains(entry.getKey())) {
        audienceList.add(entry.getValue());
      }
    }
    if (audienceList.isEmpty()) {
      audienceList.add(annotationMap.get("USER"));
    }
    var priority = rawAnnotations.replaceAll(".*priority=([0-9.]+).*", "$1");
    var lastMod =
        rawAnnotations.contains("lastModified=")
            ? string(rawAnnotations.replaceAll(".*lastModified=\"([^\"]+)\".*", "$1"))
            : "null";

    return new McpAnnotation(String.join(", ", audienceList), lastMod.toString(), priority);
  }

  private McpToolAnnotation parseToolAnnotation() {
    String rawAnnotations =
        extractAnnotationValue("io.jooby.annotation.mcp.McpTool", "annotations");

    if (rawAnnotations == null || rawAnnotations.isEmpty()) {
      return null;
    }

    // Default values matching the @McpAnnotations interface
    String readOnlyHint = "false";
    String destructiveHint = "true";
    String idempotentHint = "false";
    String openWorldHint = "true";

    if (rawAnnotations.contains("readOnlyHint=")) {
      readOnlyHint = rawAnnotations.replaceAll(".*readOnlyHint=(true|false).*", "$1");
    }
    if (rawAnnotations.contains("destructiveHint=")) {
      destructiveHint = rawAnnotations.replaceAll(".*destructiveHint=(true|false).*", "$1");
    }
    if (rawAnnotations.contains("idempotentHint=")) {
      idempotentHint = rawAnnotations.replaceAll(".*idempotentHint=(true|false).*", "$1");
    }
    if (rawAnnotations.contains("openWorldHint=")) {
      openWorldHint = rawAnnotations.replaceAll(".*openWorldHint=(true|false).*", "$1");
    }

    return new McpToolAnnotation(readOnlyHint, destructiveHint, idempotentHint, openWorldHint);
  }

  private record McpToolAnnotation(
      String readOnlyHint, String destructiveHint, String idempotentHint, String openWorldHint) {}

  private record McpAnnotation(String audience, String lastModified, String priority) {}
}
