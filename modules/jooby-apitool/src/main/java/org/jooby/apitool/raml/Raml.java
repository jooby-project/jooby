package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.internal.MoreTypes;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import org.jooby.MediaType;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteParameter;
import org.jooby.apitool.RouteResponse;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Raml {
  private String title;

  private String version;

  private String baseUri;

  private List<String> mediaType;

  private List<String> protocols;

  private Map<String, RamlType> types;

  private Map<String, RamlPath> resources = new LinkedHashMap<>();

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getBaseUri() {
    return baseUri;
  }

  public void setBaseUri(final String baseUri) {
    this.baseUri = baseUri;
  }

  public List<String> getMediaType() {
    return mediaType;
  }

  public void setMediaType(final List<String> mediaType) {
    this.mediaType = mediaType;
  }

  public List<String> getProtocols() {
    return protocols;
  }

  public void setProtocols(final List<String> protocols) {
    this.protocols = protocols;
  }

  public Map<String, RamlType> getTypes() {
    return types;
  }

  @JsonAnyGetter
  public Map<String, RamlPath> getResources() {
    return resources;
  }

  public RamlPath path(String pattern) {
    RamlPath path = resources.get(pattern);
    if (path == null) {
      path = new RamlPath();
      resources.put(pattern, path);
    }
    return path;
  }

  public RamlType define(Type javaType, RamlType baseType) {
    Objects.requireNonNull(javaType, "Java type required.");
    Objects.requireNonNull(baseType, "Raml type required.");
    if (types == null) {
      types = new LinkedHashMap<>();
    }
    String typeName = MoreTypes.getRawType(javaType).getSimpleName();
    RamlType ramlType = new RamlType(baseType.getType(), typeName);
    types.put(typeName, ramlType);
    return ramlType;
  }

  public RamlType type(Type type) {
    if (types == null) {
      types = new LinkedHashMap<>();
    }
    Type componentType = componentType(type);
    String typeName = MoreTypes.getRawType(componentType).getSimpleName();
    RamlType ramlType = RamlType.valueOf(typeName);
    if (ramlType.isObject()) {
      RamlType existing = types.get(typeName);
      if (existing == null) {
        ModelConverters converter = ModelConverters.getInstance();
        Property property = converter.readAsProperty(componentType);

        Map<PropertyBuilder.PropertyId, Object> args = new EnumMap<>(
            PropertyBuilder.PropertyId.class);
        for (Map.Entry<String, Model> entry : converter.readAll(componentType).entrySet()) {
          define(entry.getKey(), entry.getValue());
        }
        ramlType = define(typeName, PropertyBuilder.toModel(PropertyBuilder.merge(property, args)));
      } else {
        ramlType = existing;
      }
    }

    return type != componentType ? ramlType.toArray() : ramlType;
  }

  private Type componentType(final Type type) {
    Class<?> rawType = MoreTypes.getRawType(type);
    if (rawType.isArray()) {
      return rawType.getComponentType();
    }
    if (Collection.class.isAssignableFrom(rawType) && type instanceof ParameterizedType) {
      return ((ParameterizedType) type).getActualTypeArguments()[0];
    }
    return type;
  }

  private RamlType define(final String type, final Model model) {
    RamlType definition = types.get(type);
    if (definition == null) {
      RamlType object = new RamlType("object", type);
      types.put(type, object);
      Map<String, Object> example = new LinkedHashMap<>();
      Optional.ofNullable(model.getProperties())
          .ifPresent(properties -> properties.forEach((name, property) -> {
            if (property instanceof RefProperty) {
              String propertyType = propertyType(property);
              object.newProperty(name, propertyType, false);
            } else if (property instanceof ArrayProperty) {
              String propertyType = propertyType(((ArrayProperty) property).getItems()) + "[]";
              object.newProperty(name, propertyType, false);
            } else {
              RamlType ramlType = RamlType.valueOf(property.getType());
              List<String> enums = null;
              if (property instanceof StringProperty) {
                enums = ((StringProperty) property).getEnum();
              }
              String propertyType = ramlType.isObject() ? property.getType() : ramlType.getType();
              object.newProperty(name, propertyType, false,
                  Optional.ofNullable(enums).map(it -> it.toArray(new String[it.size()]))
                      .orElse(new String[0]));
              example.put(name, Optional.ofNullable(enums).<Object>map(it -> it.get(0))
                  .orElse(ramlType.getExample()));
            }
          }));
      definition = object;
      object.setExample(example);
    }
    return definition;
  }

  private String propertyType(Property property) {
    if (property instanceof RefProperty) {
      return ((RefProperty) property).getSimpleRef();
    }
    return property.getType();
  }

  public void setResources(final Map<String, RamlPath> resources) {
    this.resources = resources;
  }

  public String toYaml() throws IOException {
    YAMLMapper mapper = new YAMLMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, false);
    mapper.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
    return "#%RAML 1.0\n" + mapper.writer().withDefaultPrettyPrinter().writeValueAsString(this);
  }

  public static Raml build(Raml base, List<RouteMethod> routes) {
    Raml raml = Optional.ofNullable(base).orElseGet(Raml::new);
    BiFunction<RamlPath, String, RamlPath> pathFactory = (path, segment) ->
        path == null ? raml.path(segment) : path.path(segment);

    BiConsumer<Function<String, RamlParameter>, RouteParameter> parameterFactory = (factory, parameter) -> {
      RamlParameter p = factory.apply(parameter.name());
      p.setDescription(parameter.description()
          .map(Raml::yamlText)
          .orElse(null));
      List<String> enums = parameter.enums();
      if (enums.size() > 0) {
        p.setType(RamlType.STRING);
        p.setEnum(enums);
      } else {
        p.setType(raml.type(parameter.type()));
      }
      p.setRequired(!parameter.optional());
      p.setDefault(parameter.value());
    };

    for (RouteMethod route : routes) {
      List<String> segments = Splitter.on("/")
          .trimResults()
          .omitEmptyStrings()
          .splitToList(route.pattern());
      RamlPath path = null;
      for (String segment : segments) {
        RamlPath newPath = pathFactory.apply(path, "/" + segment);
        if (segment.startsWith("{") && segment.endsWith("}")) {
          String pathvar = segment.substring(1, segment.length() - 1);
          route.parameters(RouteParameter.Kind.PATH).stream()
              .filter(it -> it.name().equals(pathvar))
              .findFirst()
              .ifPresent(it -> parameterFactory.accept(newPath::uriParameter, it));
        }
        path = newPath;
      }
      path = Optional.ofNullable(path).orElseGet(() -> raml.path("/"));
      path.setDescription(route.summary()
          .map(Raml::yamlText)
          .orElse(null));
      RamlMethod method = path.method(route.method());
      method.setDescription(route.description()
          .map(Raml::yamlText)
          .orElse(null));

      /** Check for files, if we find one use multipart to render them: */
      List<RouteParameter> files = (route.parameters(RouteParameter.Kind.FILE));
      if (files.size() > 0) {
        route.parameters(RouteParameter.Kind.QUERY)
            .forEach(it -> parameterFactory.accept(method::formParameter, it));
        files.forEach(it -> {
          parameterFactory.accept(method::formParameter, it);
          method.setMediaType(ImmutableList.of(MediaType.multipart.name()));
        });
      } else {
        route.parameters(RouteParameter.Kind.QUERY)
            .forEach(it -> parameterFactory.accept(method::queryParameter, it));
      }
      /** Headers: */
      route.parameters(RouteParameter.Kind.HEADER)
          .forEach(it -> parameterFactory.accept(method::headerParameter, it));

      /** Body: */
      route.parameters(RouteParameter.Kind.BODY)
          .forEach(it -> {
            method.setMediaType(route.consumes());
            method.setBody(raml.type(it.type()));
          });
      /** Response: */
      RouteResponse returns = route.response();
      Map<Integer, String> status = returns.status();
      Integer statusCode = returns.statusCode();
      RamlResponse response = method.response(statusCode);
      response.setDescription(yamlText(returns.description().orElse(status.get(statusCode))));
      if (route.produces().size() > 0) {
        route.produces().forEach(type -> response.setMediaType(type, raml.type(returns.type())));
      } else {
        response.setMediaType(null, raml.type(returns.type()));
      }
      status.entrySet().stream()
          .filter(it -> !statusCode.equals(it.getKey()))
          .forEach(it -> method.response(it.getKey()).setDescription(it.getValue()));
    }

    /** Default consumes/produces: */
    List<String> alltypes = new ArrayList<>();
    Consumer<Function<RouteMethod, List<String>>> mediaTypes = types ->
        routes.stream().forEach(r -> types.apply(r).forEach(alltypes::add));
    mediaTypes.accept(RouteMethod::consumes);
    mediaTypes.accept(RouteMethod::produces);
    if (alltypes.size() == 0) {
      raml.setMediaType(ImmutableList.of(MediaType.json.name()));
    } else if (alltypes.size() == 1) {
      raml.setMediaType(alltypes);
    }

    return raml;
  }

  static String yamlText(String text) {
    return Optional.ofNullable(text).map(lines ->
        Splitter.on("\n")
            .trimResults()
            .omitEmptyStrings()
            .splitToList(lines)
            .stream()
            .collect(Collectors.joining("\n"))
    ).orElse(null);
  }
}
