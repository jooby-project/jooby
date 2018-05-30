package org.jooby.apitool;

import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.jooby.internal.apitool.SwaggerBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ApiToolFeature {

  public String yaml(Swagger swagger) throws Exception {
    return yaml(swagger, false);
  }

  public String yaml(Swagger swagger, boolean print) throws Exception {
    String yaml = Yaml.mapper().writer().withDefaultPrettyPrinter()
        .writeValueAsString(swagger);
    if (print) {
      System.out.println(yaml);
    }
    return yaml;
  }

  public String json(Swagger swagger) throws Exception {
    return Json.mapper().writer().withDefaultPrettyPrinter()
        .writeValueAsString(swagger);
  }

  public Swagger swagger(List<RouteMethod> routes) throws Exception {
    return new SwaggerBuilder(ApiTool.DEFAULT_TAGGER).build(null, routes);
  }

  public Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
