/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import io.jooby.MediaType;
import io.swagger.v3.core.util.RefUtils;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.servers.Servers;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.jooby.internal.openapi.AsmUtils.annotationList;
import static io.jooby.internal.openapi.AsmUtils.annotationValue;
import static io.jooby.internal.openapi.AsmUtils.boolValue;
import static io.jooby.internal.openapi.AsmUtils.enumValue;
import static io.jooby.internal.openapi.AsmUtils.intValue;
import static io.jooby.internal.openapi.AsmUtils.stringList;
import static io.jooby.internal.openapi.AsmUtils.toMap;
import static io.jooby.internal.openapi.AsmUtils.findAnnotationByType;
import static io.jooby.internal.openapi.AsmUtils.stringValue;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Complement openAPI output with swagger annotations.
 */
public class OpenAPIParser {
  public static void parse(ParserContext ctx, OpenAPIExt openapi) {
    Type type = Type.getObjectType(openapi.getSource().replace(".", "/"));
    ClassNode node = ctx.classNode(type);

    findAnnotationByType(node.visibleAnnotations, OpenAPIDefinition.class)
        .stream()
        .findFirst()
        .ifPresent(a -> definition(openapi, a));

    findAnnotationByType(node.visibleAnnotations, Servers.class)
        .stream()
        .map(it -> annotationList(toMap(it), "value"))
        .forEach(it -> servers(it, openapi::addServersItem));
    findAnnotationByType(node.visibleAnnotations, Server.class)
        .stream()
        .findFirst()
        .ifPresent(it -> servers(singletonList(toMap(it)), openapi::addServersItem));
  }

  public static void parse(ParserContext ctx, OperationExt operation) {
    /** Tags: */
    MethodNode method = operation.getNode();
    List<AnnotationNode> annotations = operation.getAllAnnotations();

    findAnnotationByType(annotations, Tags.class).stream()
        .map(it -> annotationList(toMap(it), "value"))
        .forEach(a -> tags(a, operation::addTag));
    findAnnotationByType(annotations, Tag.class)
        .stream()
        .findFirst()
        .ifPresent(it -> tags(singletonList(toMap(it)), operation::addTag));

    /** @Operation: */
    findAnnotationByType(method.visibleAnnotations, Operation.class).stream()
        .findFirst()
        .ifPresent(a -> operation(ctx, operation, toMap(a)));

    /** SecurityRequirements: */
    findAnnotationByType(method.visibleAnnotations, SecurityRequirements.class).stream()
        .map(it -> annotationList(toMap(it), "value"))
        .forEach(a -> securityRequirements(a, operation::addSecurityItem));

    findAnnotationByType(method.visibleAnnotations, SecurityRequirement.class)
        .stream()
        .findFirst()
        .ifPresent(a -> securityRequirements(singletonList(toMap(a)), operation::addSecurityItem));

    /** @ApiResponses: */
    findAnnotationByType(method.visibleAnnotations, ApiResponses.class)
        .stream()
        .flatMap(it -> annotationList(toMap(it), "value").stream())
        .forEach(a -> operationResponse(ctx, operation, a));

    /** @ApiResponse: */
    findAnnotationByType(method.visibleAnnotations, ApiResponse.class)
        .stream()
        .findFirst()
        .ifPresent(a -> operationResponse(ctx, operation, toMap(a)));

    checkDefaultResponse(operation);
  }

  private static void servers(List<Map<String, Object>> serverList,
      Consumer<io.swagger.v3.oas.models.servers.Server> consumer) {
    for (Map<String, Object> serverMap : serverList) {
      io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
      stringValue(serverMap, "url", server::setUrl);
      stringValue(serverMap, "description", server::setDescription);
      annotationList(serverMap, "variables", variableList -> {
        ServerVariables variables = new ServerVariables();
        for (Map<String, Object> varMap : variableList) {
          io.swagger.v3.oas.models.servers.ServerVariable variable = new io.swagger.v3.oas.models.servers.ServerVariable();
          stringValue(varMap, "description", variable::setDescription);
          stringValue(varMap, "defaultValue", variable::setDefault);
          stringList(varMap, "allowableValues", variable::setEnum);
          variables.put((String) varMap.get("name"), variable);
        }
        server.setVariables(variables);
      });
      consumer.accept(server);
    }
  }

  private static void definition(OpenAPIExt openapi, AnnotationNode node) {
    Map<String, Object> annotation = toMap(node);
    if (openapi.getInfo() == null) {
      annotationValue(annotation, "info", infoMap -> {
        io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();

        stringValue(infoMap, "title", info::setTitle);
        stringValue(infoMap, "description", info::setDescription);
        stringValue(infoMap, "termsOfService", info::setTermsOfService);
        stringValue(infoMap, "version", info::setVersion);

        annotationValue(infoMap, "contact", map -> {
          io.swagger.v3.oas.models.info.Contact contact = new io.swagger.v3.oas.models.info.Contact();
          stringValue(map, "name", contact::setName);
          stringValue(map, "url", contact::setUrl);
          stringValue(map, "email", contact::setEmail);
          info.setContact(contact);
        });

        annotationValue(infoMap, "license", map -> {
          io.swagger.v3.oas.models.info.License license = new io.swagger.v3.oas.models.info.License();
          stringValue(map, "name", license::setName);
          stringValue(map, "url", license::setUrl);
          info.setLicense(license);
        });

        openapi.setInfo(info);
      });
    }
    // Tags
    if (openapi.getTags() == null || openapi.getTags().isEmpty()) {
      annotationList(annotation, "tags", tagList ->
          tags(tagList, openapi::addTagsItem)
      );
    }
    // Server
    if (openapi.getServers() == null || openapi.getServers().isEmpty()) {
      annotationList(annotation, "servers", serverList ->
          servers(serverList, openapi::addServersItem)
      );
    }

    // Security
    if (openapi.getSecurity() == null || openapi.getSecurity().isEmpty()) {
      annotationList(annotation, "security", securityList ->
          securityRequirements(securityList, openapi::addSecurityItem)
      );
    }
  }

  private static void tags(List<Map<String, Object>> tags,
      Consumer<io.swagger.v3.oas.models.tags.Tag> consumer) {
    for (Map<String, Object> tagMap : tags) {
      io.swagger.v3.oas.models.tags.Tag tag = new io.swagger.v3.oas.models.tags.Tag();
      stringValue(tagMap, "name", tag::setName);
      stringValue(tagMap, "description", tag::setDescription);
      consumer.accept(tag);
    }
  }

  /**
   * This method removes the default response when there is an explicit success response.
   * @param operation
   */
  private static void checkDefaultResponse(OperationExt operation) {
    if (!operation.getResponseCodes().contains("200") &&
        operation.getResponses().keySet().stream().filter(StatusCodeParser::isSuccessCode).count()
            > 1) {
      operation.getResponses().remove("200");
    }
  }

  private static void operation(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    stringValue(annotation, "operationId", operation::setOperationId);

    stringValue(annotation, "method", operation::setMethod);

    boolValue(annotation, "deprecated", operation::setDeprecated);

    boolValue(annotation, "hidden", operation::setHidden);

    stringValue(annotation, "summary", operation::setSummary);

    stringValue(annotation, "description", operation::setDescription);

    stringList(annotation, "tags", tags -> tags.forEach(operation::addTagsItem));

    annotationList(annotation, "servers", servers -> servers(servers, operation::addServersItem));

    annotationList(annotation, "security",
        values -> securityRequirements(values, operation::addSecurityItem));

    annotationList(annotation, "parameters", values -> parameters(ctx, operation, values));

    requestBody(ctx, operation, toMap((AnnotationNode) annotation.get("requestBody")));

    responses(ctx, operation, annotation);
  }

  private static void securityRequirements(List<Map<String, Object>> securityRequirements,
      Consumer<io.swagger.v3.oas.models.security.SecurityRequirement> consumer) {
    for (Map<String, Object> securityMap : securityRequirements) {
      String name = (String) securityMap.get("name");
      List<String> scopes = (List<String>) securityMap
          .getOrDefault("scopes", Collections.emptyList());
      io.swagger.v3.oas.models.security.SecurityRequirement requirement = new io.swagger.v3.oas.models.security.SecurityRequirement();
      requirement.addList(name, scopes);

      consumer.accept(requirement);
    }
  }

  private static void requestBody(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    if (annotation.size() > 0) {
      RequestBodyExt requestBody = operation.getRequestBody();
      if (requestBody == null) {
        requestBody = new RequestBodyExt();
        operation.setRequestBody(requestBody);
      }
      stringValue(annotation, "description", requestBody::setDescription);
      boolValue(annotation, "required", requestBody::setRequired);
      String defaultMediaType = operation.getConsumes().stream().findFirst().orElse(MediaType.JSON);
      content(ctx, defaultMediaType, annotation).ifPresent(requestBody::setContent);
    }
  }

  @io.swagger.v3.oas.annotations.Operation(parameters =
  @Parameter(
      name = "p",
      description = "des",
      in = ParameterIn.COOKIE,
      required = false,
      deprecated = true,
      allowEmptyValue = true,
      allowReserved = false,
      hidden = false,
      explode = Explode.TRUE,
      ref = "Pet"
  )
  )
  private static void parameters(ParserContext ctx, OperationExt operation,
      List<Map<String, Object>> parameters) {
    for (int i = 0; i < parameters.size(); i++) {
      Map<String, Object> parameterMap = parameters.get(i);
      String name = (String) parameterMap.get("name");
      io.swagger.v3.oas.models.parameters.Parameter parameter;
      if (name != null) {
        int index = i;
        parameter = operation.getParameters().stream()
            .filter(it -> it.getName().equals(name))
            .findFirst()
            .orElseGet(() -> operation.getParameter(index));
      } else {
        parameter = operation.getParameter(i);
      }
      if (parameter == null) {
        throw new IllegalArgumentException(
            "Parameter not found: " + name + " at  position: " + i + " for annotation: "
                + parameterMap);
      }
      Optional.ofNullable(name).ifPresent(parameter::setName);
      stringValue(parameterMap, "description", parameter::setDescription);
      enumValue(parameterMap, "in", in -> parameter.setIn(in.toLowerCase()));
      boolValue(parameterMap, "required", parameter::setRequired);
      boolValue(parameterMap, "deprecated", parameter::setDeprecated);
      boolValue(parameterMap, "allowEmptyValue", parameter::setAllowEmptyValue);
      boolValue(parameterMap, "allowReserved", parameter::setAllowReserved);
      // NOTE: Hidden is not present on parameter
      //boolValue(parameterMap, "hidden", parameter::setHidden);
      enumValue(parameterMap, "explode", value -> parameter.setExample(Boolean.valueOf(value)));
      stringValue(parameterMap, "ref", ref -> parameter.set$ref(RefUtils.constructRef(ref)));
      arrayOrSchema(ctx, parameterMap).ifPresent(parameter::setSchema);
      examples(parameterMap, parameter::setExample, parameter::setExamples);
    }
  }

  private static void examples(Map<String, Object> annotation, Consumer<String> exampleConsumer,
      Consumer<Map<String, Example>> consumer) {
    Map<String, Example> examples = new LinkedHashMap<>();
    stringValue(annotation, "example", exampleConsumer);

    annotationList(annotation, "examples", values -> {
      for (Map<String, Object> e : values) {
        Example example = new Example();
        stringValue(e, "summary", example::setSummary);
        stringValue(e, "description", example::setDescription);
        stringValue(e, "value", example::setValue);
        stringValue(e, "externalValue", example::setExternalValue);
        String key = (String) e.getOrDefault("name", "example" + examples.size());
        examples.put(key, example);
      }
      consumer.accept(examples);
    });
  }

  private static void responses(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    annotationList(annotation, "responses", values ->
        values.forEach(it -> operationResponse(ctx, operation, it))
    );
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse)
  private static void operationResponse(ParserContext ctx, OperationExt operation,
      Map<String, Object> annotation) {
    String code = stringValue(annotation, "responseCode", "200")
        .replace("default", "200");

    ResponseExt response = operation.addResponse(code);

    annotationList(annotation, "headers", values -> {
      Map<String, Header> headers = new LinkedHashMap<>();
      for (Map<String, Object> value : values) {
        Header header = new Header();

        String name = stringValue(value, "name");
        stringValue(value, "description", header::setDescription);
        io.swagger.v3.oas.models.media.Schema schema = annotationValue(value, "schema")
            .map(schemaMap -> toSchema(ctx, schemaMap).orElseGet(StringSchema::new))
            .orElseGet(StringSchema::new);
        header.setSchema(schema);
        headers.put(name, header);
      }
      response.setHeaders(headers);
    });

    stringValue(annotation, "description", response::setDescription);

    String defaultMediaType = operation.getProduces().stream().findFirst().orElse(MediaType.JSON);
    content(ctx, defaultMediaType, annotation).ifPresent(response::setContent);
  }

  @io.swagger.v3.oas.annotations.Operation(responses = @ApiResponse(content = @Content))
  private static Optional<io.swagger.v3.oas.models.media.Content> content(ParserContext ctx,
      String defaultMediaType, Map<String, Object> annotation) {
    io.swagger.v3.oas.models.media.Content content = new io.swagger.v3.oas.models.media.Content();
    annotationList(annotation, "content", values -> {
      for (Map<String, Object> value : values) {
        mediaType(ctx, content, defaultMediaType, value);
      }
    });

    return content.isEmpty() ? Optional.empty() : Optional.of(content);
  }

  @ApiResponse(
      content = @Content(
          mediaType = "media/type",
          array = @ArraySchema(schema = @Schema(implementation = String.class))
      )
  )
  private static void mediaType(ParserContext ctx, io.swagger.v3.oas.models.media.Content content,
      String defaultMediaType, Map<String, Object> contentMap) {
    if (contentMap == null || contentMap.isEmpty()) {
      return;
    }

    String mediaType = stringValue(contentMap, "mediaType", defaultMediaType);

    Optional<io.swagger.v3.oas.models.media.Schema> schema = arrayOrSchema(ctx, contentMap);

    io.swagger.v3.oas.models.media.MediaType mediaTypeObject = new io.swagger.v3.oas.models.media.MediaType();
    schema.ifPresent(mediaTypeObject::setSchema);

    content.addMediaType(mediaType, mediaTypeObject);
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> arrayOrSchema(ParserContext ctx,
      Map<String, Object> annotation) {
    AnnotationNode e = (AnnotationNode) annotation.get("array");
    if (e != null) {
      return toArraySchema(ctx, toMap(e));
    } else {
      return toSchema(ctx, toMap((AnnotationNode) annotation.get("schema")));
    }
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> toArraySchema(ParserContext ctx,
      Map<String, Object> annotation) {
    io.swagger.v3.oas.models.media.ArraySchema arraySchema = new io.swagger.v3.oas.models.media.ArraySchema();
    boolValue(annotation, "uniqueItems", arraySchema::setUniqueItems);
    intValue(annotation, "maxItems", arraySchema::setMaxItems);
    intValue(annotation, "minItems", arraySchema::setMinItems);
    if (annotation.containsKey("arraySchema")) {
      toArraySchema(ctx, toMap((AnnotationNode) annotation.get("arraySchema")))
          .ifPresent(arraySchema::setItems);
    } else {
      toSchema(ctx, toMap((AnnotationNode) annotation.get("schema")))
          .ifPresent(arraySchema::setItems);
    }
    return Optional.of(arraySchema);
  }

  private static Optional<io.swagger.v3.oas.models.media.Schema> toSchema(ParserContext ctx,
      Map<String, Object> annotation) {
    Map<String, List<io.swagger.v3.oas.models.media.Schema>> schemaMap = new HashMap<>();

    schemaType(ctx, annotation, "implementation", schemaMap::put);
    schemaType(ctx, annotation, "not", schemaMap::put);
    schemaType(ctx, annotation, "anyOf", schemaMap::put);
    schemaType(ctx, annotation, "oneOf", schemaMap::put);
    schemaType(ctx, annotation, "allOf", schemaMap::put);

    if (schemaMap.isEmpty()) {
      return Optional.empty();
    }

    List<io.swagger.v3.oas.models.media.Schema> schemas = schemaMap.get("implementation");
    io.swagger.v3.oas.models.media.Schema schema;
    if (schemas.isEmpty()) {
      ComposedSchema composedSchema = new ComposedSchema();

      Optional.ofNullable(schemaMap.get("anyOf")).ifPresent(composedSchema::anyOf);
      Optional.ofNullable(schemaMap.get("oneOf")).ifPresent(composedSchema::oneOf);
      Optional.ofNullable(schemaMap.get("allOf")).ifPresent(composedSchema::allOf);

      schema = composedSchema;
    } else {
      schema = schemas.get(0);
    }

    Optional.ofNullable(schemaMap.get("not")).ifPresent(not -> schema.not(not.get(0)));

    return Optional.of(schema);
  }

  private static void schemaType(ParserContext ctx, Map<String, Object> schema, String property,
      BiConsumer<String, List<io.swagger.v3.oas.models.media.Schema>> consumer) {
    Object value = schema.get(property);
    List<Type> types;
    if (value instanceof List) {
      types = (List) value;
    } else if (value instanceof Type) {
      types = Collections.singletonList((Type) value);
    } else {
      types = Collections.emptyList();
    }
    if (types.size() > 0) {
      List<io.swagger.v3.oas.models.media.Schema> schemas = types.stream()
          .map(Type::getClassName)
          .map(ctx::schema)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      consumer.accept(property, schemas);
    }
  }
}
