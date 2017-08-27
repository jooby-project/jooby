package org.jooby.internal.apitool;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.PropertyBuilder.PropertyId;
import org.jooby.MediaType;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteParameter;
import org.jooby.apitool.RouteResponse;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SwaggerBuilder {
  private static final Pattern TAG = Pattern.compile("(api)|/");

  private static final Function<RouteMethod, String> TAG_PROVIDER = r -> {
    Iterator<String> segments = Splitter.on(TAG)
        .trimResults()
        .omitEmptyStrings()
        .split(r.pattern())
        .iterator();
    return segments.hasNext() ? segments.next() : "";
  };
  private Function<RouteMethod, String> tagger = TAG_PROVIDER;

  public SwaggerBuilder() {
  }

  public SwaggerBuilder groupBy(Function<RouteMethod, String> tag) {
    this.tagger = tag;
    return this;
  }

  public Swagger build(Swagger base, final List<RouteMethod> routes) throws Exception {
    Swagger swagger = Optional.ofNullable(base).orElseGet(Swagger::new);

    /** Tags: */
    Function<String, Tag> tagFactory = value ->
        Optional.ofNullable(swagger.getTag(value))
            .orElseGet(() -> {
              Tag tag = new Tag();
              if (value.length() > 0) {
                tag.name(Character.toUpperCase(value.charAt(0)) + value.substring(1));
              } else {
                tag.name(value);
              }
              swagger.addTag(tag);
              return tag;
            });

    /** Paths: */
    Function<String, Path> pathFactory = pattern ->
        Optional.ofNullable(swagger.getPath(pattern))
            .orElseGet(() -> {
              Path path = new Path();
              swagger.path(pattern, path);
              return path;
            });

    ModelConverters converter = ModelConverters.getInstance();
    /** Model factory: */
    Function<Type, Model> modelFactory = type -> {
      Property property = converter.readAsProperty(type);

      Map<PropertyId, Object> args = new EnumMap<>(PropertyId.class);
      for (Map.Entry<String, Model> entry : converter.readAll(type).entrySet()) {
        swagger.addDefinition(entry.getKey(), entry.getValue());
      }
      return PropertyBuilder.toModel(PropertyBuilder.merge(property, args));
    };

    for (RouteMethod route : routes) {
      /** Find or create tag: */
      Tag tag = tagFactory.apply(this.tagger.apply(route));
      // groupBy summary
      route.summary().ifPresent(tag::description);

      /** Find or create path: */
      Path path = pathFactory.apply(route.pattern());

      /** Operation: */
      Operation op = new Operation();
      op.addTag(tag.getName());
      op.operationId(route.name().orElseGet(
          () -> route.method().toLowerCase() + tag.getName() + route.parameters().stream()
              .filter(it -> route.method().equalsIgnoreCase("get")
                  && it.kind() == RouteParameter.Kind.PATH)
              .findFirst()
              .map(it -> "By" + LOWER_CAMEL.to(UPPER_CAMEL, it.name()))
              .orElse("")));

      /** Doc and summary: */
      route.description().ifPresent(description -> {
        int dot = description.indexOf('.');
        if (dot > 0) {
          op.summary(description.substring(0, dot));
        }
        op.description(description);
      });
      route.response().description()
          .ifPresent(returns -> (Strings.nullToEmpty(op.getDescription()) + " " + returns).trim());

      /** Consumes/Produces . */
      route.consumes().forEach(op::addConsumes);
      route.produces().forEach(op::addProduces);

      /** Parameters: */
      route.parameters().stream().map(it -> {
        Type type = it.type();
        final Property property = converter.readAsProperty(type);
        Parameter parameter = it.accept(new RouteParameter.Visitor<Parameter>() {
          @Override public Parameter visitBody(final RouteParameter parameter) {
            return new BodyParameter().schema(modelFactory.apply(parameter.type()));
          }

          @Override public Parameter visitFile(final RouteParameter parameter) {
            return complement(property, parameter, new FormParameter());
          }

          @Override public Parameter visitForm(final RouteParameter parameter) {
            return complement(property, parameter, new FormParameter());
          }

          @Override public Parameter visitHeader(final RouteParameter parameter) {
            return complement(property, parameter, new HeaderParameter());
          }

          @Override public Parameter visitPath(final RouteParameter parameter) {
            return complement(property, parameter, new PathParameter());
          }

          @Override public Parameter visitQuery(final RouteParameter parameter) {
            return complement(property, parameter, new QueryParameter());
          }
        });
        if (it.kind() == RouteParameter.Kind.FILE) {
          op.setConsumes(ImmutableList.of(MediaType.multipart.name()));
        }
        parameter.setName(it.name());
        parameter.setRequired(!it.optional());
        parameter.setDescription(property.getDescription());
        it.description().ifPresent(parameter::setDescription);
        return parameter;
      }).forEach(op::addParameter);

      /** Response: */
      RouteResponse returns = route.response();
      Map<Integer, String> status = returns.status();
      Integer statusCode = returns.statusCode();
      Response response = new Response();
      String doc = returns.description().orElse(status.get(statusCode));
      response.description(doc);
      // make sure type definition gets in
      modelFactory.apply(returns.type());
      response.schema(converter.readAsProperty(returns.type()));
      op.addResponse(statusCode.toString(), response);
      status.entrySet().stream()
          .filter(it -> !statusCode.equals(it.getKey()))
          .forEach(it -> op.addResponse(it.getKey().toString(), new Response()
              .description(it.getValue())));

      /** Done: */
      path.set(route.method().toLowerCase(), op);
    }
    Function<Function<RouteMethod, List<String>>, List<String>> mediaTypes = types ->
        routes.stream().flatMap(it -> types.apply(it).stream()).collect(Collectors.toList());
    /** Default consumes/produces: */
    List<String> consumes = mediaTypes.apply(RouteMethod::consumes);
    if (consumes.size() == 0) {
      swagger.consumes(MediaType.json.name());
    } else if (consumes.size() == 1) {
      swagger.consumes(consumes);
    }
    List<String> produces = mediaTypes.apply(RouteMethod::produces);
    if (produces.size() == 0) {
      swagger.produces(MediaType.json.name());
    } else if (produces.size() == 1) {
      swagger.produces(produces);
    }
    return swagger;
  }

  private SerializableParameter complement(Property property, RouteParameter source,
      SerializableParameter param) {
    param.setType(property.getType());
    param.setFormat(property.getFormat());
    if (property instanceof ArrayProperty) {
      param.setItems(((ArrayProperty) property).getItems());
    }
    List<String> enums = source.enums();
    if (enums.size() > 0) {
      param.setEnum(enums);
    }
    return param;
  }
}
