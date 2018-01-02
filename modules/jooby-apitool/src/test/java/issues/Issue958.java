package issues;

import com.google.inject.util.Types;
import io.swagger.models.HttpMethod;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.FormParameter;
import org.jooby.Upload;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteParameter;
import org.jooby.apitool.RouteResponse;
import org.jooby.internal.apitool.SwaggerBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;

public class Issue958 {

  @Test
  public void shouldCreateSwaggerFile() throws Exception {
    Swagger swagger = new SwaggerBuilder()
        .build(null, Arrays.asList(route(fileParam("myfile", Upload.class))));
    FormParameter parameter = (FormParameter) swagger
        .getPath("/file")
        .getOperationMap()
        .get(HttpMethod.POST)
        .getParameters()
        .get(0);
    assertEquals("formData", parameter.getIn());
    assertEquals("myfile", parameter.getName());
    assertEquals("file", parameter.getType());
  }

  @Test
  public void shouldCreateSwaggerFiles() throws Exception {
    Swagger swagger = new SwaggerBuilder()
        .build(null, Arrays.asList(route(fileParam("files", Types.listOf(Upload.class)))));
    FormParameter parameter = (FormParameter) swagger
        .getPath("/file")
        .getOperationMap()
        .get(HttpMethod.POST)
        .getParameters()
        .get(0);
    assertEquals("formData", parameter.getIn());
    assertEquals("files", parameter.getName());
    assertEquals("array", parameter.getType());
    assertEquals("file", parameter.getItems().getType());
  }

  @Test
  public void shouldCreateSwaggerFileSet() throws Exception {
    Swagger swagger = new SwaggerBuilder()
        .build(null, Arrays.asList(route(fileParam("fset", Types.setOf(Upload.class)))));
    FormParameter parameter = (FormParameter) swagger
        .getPath("/file")
        .getOperationMap()
        .get(HttpMethod.POST)
        .getParameters()
        .get(0);
    assertEquals("formData", parameter.getIn());
    assertEquals("fset", parameter.getName());
    assertEquals("array", parameter.getType());
    assertEquals("file", parameter.getItems().getType());
  }

  private RouteMethod route(RouteParameter... params) {
    RouteMethod method = new RouteMethod("POST", "/file", new RouteResponse(String.class));
    method.parameters(Arrays.asList(params));
    return method;
  }

  private RouteParameter fileParam(String name, Type type) {
    RouteParameter param = new RouteParameter(name, RouteParameter.Kind.FILE, type, null);
    return param;
  }
}
