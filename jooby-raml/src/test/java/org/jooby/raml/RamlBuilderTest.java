package org.jooby.raml;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jooby.MediaType;
import org.jooby.Upload;
import org.jooby.internal.raml.RamlBuilder;
import org.jooby.spec.RouteParam;
import org.jooby.spec.RouteParamType;
import org.jooby.spec.RouteResponse;
import org.jooby.spec.RouteSpec;
import org.junit.Test;

import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class RamlBuilderTest {

  public static class Param implements RouteParam {

    private String name;

    private Type type;

    private RouteParamType paramType;

    private Object value;

    public Param(final String name, final Type type, final RouteParamType paramType,
        final Object value, final String doc) {
      this.name = name;
      this.type = type;
      this.paramType = paramType;
      this.value = value;
      this.doc = doc;
    }

    public Param(final String name, final Type type, final RouteParamType paramType) {
      this(name, type, paramType, null, null);
    }

    public Param(final String name, final Type type, final RouteParamType paramType,
        final String doc) {
      this(name, type, paramType, null, doc);
    }

    private String doc;

    @Override
    public String name() {
      return name;
    }

    @Override
    public Type type() {
      return type;
    }

    @Override
    public RouteParam type(final Type type) {
      this.type = type;
      return this;
    }

    @Override
    public RouteParamType paramType() {
      return paramType;
    }

    @Override
    public RouteParam paramType(final RouteParamType type) {
      paramType = type;
      return this;
    }

    @Override
    public Object value() {
      return value;
    }

    @Override
    public Optional<String> doc() {
      return Optional.ofNullable(doc);
    }

  }

  @SuppressWarnings("serial")
  public static class Response implements RouteResponse {

    private Type type = Object.class;

    private String doc;

    private Map<Integer, String> statusCodes = new LinkedHashMap<>();

    public Response() {
    }

    @Override
    public Type type() {
      return type;
    }

    public Response type(final Type type) {
      this.type = type;
      return this;
    }

    @Override
    public Optional<String> doc() {
      return Optional.ofNullable(doc);
    }

    public Response doc(final String doc) {
      this.doc = doc;
      return this;
    }

    @Override
    public Map<Integer, String> statusCodes() {
      return statusCodes;
    }

    public Response status(final int status, final String code) {
      statusCodes.put(status, code);
      return this;
    }

  }

  @SuppressWarnings("serial")
  public static class Spec implements RouteSpec {

    String summary;

    String name;

    String method;

    String pattern;

    String doc;

    List<String> consumes = Arrays.asList("*/*");

    List<String> produces = Arrays.asList("*/*");

    List<RouteParam> params = new ArrayList<>();

    Response rsp = new Response();

    @Override
    public Optional<String> summary() {
      return Optional.ofNullable(summary);
    }

    public Spec summary(final String summary) {
      this.summary = summary;
      return this;
    }

    @Override
    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    public Spec setName(final String name) {
      this.name = name;
      return this;
    }

    @Override
    public String method() {
      return method;
    }

    public Spec method(final String method) {
      this.method = method;
      return this;
    }

    @Override
    public String pattern() {
      return pattern;
    }

    public Spec pattern(final String pattern) {
      this.pattern = pattern;
      return this;
    }

    @Override
    public Optional<String> doc() {
      return Optional.ofNullable(doc);
    }

    public Spec doc(final String doc) {
      this.doc = doc;
      return this;
    }

    @Override
    public List<String> consumes() {
      return consumes;
    }

    public Spec consumes(final String... consumes) {
      this.consumes = Arrays.asList(consumes);
      return this;
    }

    @Override
    public List<String> produces() {
      return produces;
    }

    public Spec produces(final String... produces) {
      this.produces = Arrays.asList(produces);
      return this;
    }

    @Override
    public List<RouteParam> params() {
      return params;
    }

    public Spec params(final RouteParam... params) {
      this.params = Arrays.asList(params);
      return this;
    }

    @Override
    public RouteResponse response() {
      return rsp;
    }

    public Spec rsp(final Consumer<Response> rsp) {
      rsp.accept(this.rsp);
      return this;
    }

  }

  private Config conf = ConfigFactory.empty()
      .withValue("mediaType", ConfigValueFactory.fromAnyRef("application/json"));

  @Test
  public void routes() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/api/pets"),
            route("GET", "/api/pets/:id"),
            route("POST", "/api/pets"),
            route("DELETE", "/api/pets/:id")));
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/api:\n" +
        "  /pets:\n" +
        "    get:\n" +
        "    post:\n" +
        "    /{id}:\n" +
        "      get:\n" +
        "      delete:", raml);
  }

  @Test
  public void moreRoutes() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/api/pets"),
            route("GET", "/api/pets/:id"),
            route("POST", "/api/pets"),
            route("DELETE", "/api/pets/:id"),
            route("GET", "/api/people"),
            route("GET", "/api/people/:id")));
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/api:\n" +
        "  /pets:\n" +
        "    get:\n" +
        "    post:\n" +
        "    /{id}:\n" +
        "      get:\n" +
        "      delete:\n" +
        "  /people:\n" +
        "    get:\n" +
        "    /{id}:\n" +
        "      get:", raml);
  }

  @Test
  public void moreRoutes2() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/api/pets"),
            route("GET", "/api/pets/:id"),
            route("POST", "/api/pets"),
            route("DELETE", "/api/pets/:id"),
            route("GET", "/api/people/:id")));
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/api:\n" +
        "  /pets:\n" +
        "    get:\n" +
        "    post:\n" +
        "    /{id}:\n" +
        "      get:\n" +
        "      delete:\n" +
        "  /people/{id}:\n" +
        "    get:", raml);
  }

  @Test
  public void routes2() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/api/pets")));
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/api/pets:\n" +
        "  get:", raml);
  }

  @Test
  public void uriParams() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/users/:userId", path("userId", int.class, "The id of the user"))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/users/{userId}:\n" +
        "  uriParameters:\n" +
        "    userId:\n" +
        "      type: integer\n" +
        "      description: 'The id of the user'\n" +
        "      required: true\n" +
        "  get:", raml);
  }

  @Test
  public void queryParams() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/users/:userId", path("userId", int.class, "The id of the user"),
                query("internal", int.class))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/users/{userId}:\n" +
        "  uriParameters:\n" +
        "    userId:\n" +
        "      type: integer\n" +
        "      description: 'The id of the user'\n" +
        "      required: true\n" +
        "  get:\n" +
        "    queryParameters:\n" +
        "      internal:\n" +
        "        type: integer\n" +
        "        required: true", raml);
  }

  @Test
  public void formParam() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("POST", "/users", body("userId", int.class)).consumes(MediaType.form.name())))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/users:\n" +
        "  post:\n" +
        "    body:\n" +
        "      application/x-www-form-urlencoded:\n" +
        "        formParameters:\n" +
        "          type: integer", raml);
  }

  @Test
  public void bodyParam() {
    String doc = "<p>Enters the file content for an existing song entity.</p>" +
        "  <ul>\n"
        + "  <li>Use the <code>binary/octet-stream</code> content type to specify the content from any consumer (excepting web-browsers).</li>\n"
        +
        "  <li>Use the <code>multipart-form/data</code> content type to upload a file which content will become the file-content</li>\n"
        + "</ul>\n";
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("POST", "/users", doc, body("userId", int.class)).consumes("application/json")
                .produces("application/json")))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/users:\n" +
        "  post:\n" +
        "    description: |-\n" +
        "      Enters the file content for an existing song entity. \n\n" +
        "       * Use the `binary/octet-stream` content type to specify the content from any consumer (excepting web-browsers).\n\n"
        +
        "       * Use the `multipart-form/data` content type to upload a file which content will become the file-content\n"
        +
        "    body:\n" +
        "      application/json:\n" +
        "        type: integer", raml);
  }

  @Test
  public void fileParam() {
    String doc = "<p>Enters the file content for an existing song entity.</p>" +
        "  <ul>\n"
        + "  <li>Use the <code>binary/octet-stream</code> content type to specify the content from any consumer (excepting web-browsers).</li>\n"
        +
        "  <li>Use the <code>multipart-form/data</code> content type to upload a file which content will become the file-content</li>\n"
        + "</ul>\n";
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("POST", "/users", doc, file("file"))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/users:\n" +
        "  post:\n" +
        "    description: |-\n" +
        "      Enters the file content for an existing song entity. \n\n" +
        "       * Use the `binary/octet-stream` content type to specify the content from any consumer (excepting web-browsers).\n\n"
        +
        "       * Use the `multipart-form/data` content type to upload a file which content will become the file-content\n"
        +
        "    body:\n" +
        "      multipart/form-data:\n" +
        "        formParameters:\n" +
        "          file:\n" +
        "            type: file\n" +
        "            required: true", raml);
  }

  @Test
  public void rsp() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/users/:userId", path("userId", int.class, "The id of the user"))
                .rsp(rsp -> rsp.type(Person.class).status(200, "Success"))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "types:\n" +
        "  Person:\n" +
        "    type: object\n" +
        "    properties:\n" +
        "      name:\n" +
        "        type: string\n" +
        "      parent:\n" +
        "        type: Person\n" +
        "      children:\n" +
        "        type: Person[]\n" +
        "      age:\n" +
        "        type: integer\n" +
        "        required: false\n" +
        "/users/{userId}:\n" +
        "  uriParameters:\n" +
        "    userId:\n" +
        "      type: integer\n" +
        "      description: 'The id of the user'\n" +
        "      required: true\n" +
        "  get:\n" +
        "    responses:\n" +
        "      200:\n" +
        "        body:\n" +
        "          application/json:\n" +
        "            type: Person", raml);
  }

  @Test
  public void rsp2() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/users/:userId", path("userId", int.class, "The id of the user"))
                .rsp(
                    rsp -> rsp.type(Person.class).status(200, "Success").status(404,
                        "Not found\nNextLine"))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "types:\n" +
        "  Person:\n" +
        "    type: object\n" +
        "    properties:\n" +
        "      name:\n" +
        "        type: string\n" +
        "      parent:\n" +
        "        type: Person\n" +
        "      children:\n" +
        "        type: Person[]\n" +
        "      age:\n" +
        "        type: integer\n" +
        "        required: false\n" +
        "/users/{userId}:\n" +
        "  uriParameters:\n" +
        "    userId:\n" +
        "      type: integer\n" +
        "      description: 'The id of the user'\n" +
        "      required: true\n" +
        "  get:\n" +
        "    responses:\n" +
        "      200:\n" +
        "        body:\n" +
        "          application/json:\n" +
        "            type: Person\n" +
        "      404:\n" +
        "        description: |-\n" +
        "          Not found\n" +
        "          NextLine", raml);
  }

  @Test
  public void issue534() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(route("GET", "/")));
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "/:\n" +
        "  get:", raml);
  }

  @Test
  public void rsp3() {
    String raml = new RamlBuilder(conf)
        .build(Arrays.asList(
            route("GET", "/users/:userId", path("userId", int.class, "The id of the user"))
                .rsp(rsp -> rsp.type(Types.listOf(Person.class))
                    .doc("Some doc")
                    .status(200, "Success").status(404, "Not found"))))
        .trim();
    assertEquals("#%RAML 1.0\n" +
        "mediaType: application/json\n" +
        "types:\n" +
        "  Person:\n" +
        "    type: object\n" +
        "    properties:\n" +
        "      name:\n" +
        "        type: string\n" +
        "      parent:\n" +
        "        type: Person\n" +
        "      children:\n" +
        "        type: Person[]\n" +
        "      age:\n" +
        "        type: integer\n" +
        "        required: false\n" +
        "/users/{userId}:\n" +
        "  uriParameters:\n" +
        "    userId:\n" +
        "      type: integer\n" +
        "      description: 'The id of the user'\n" +
        "      required: true\n" +
        "  get:\n" +
        "    responses:\n" +
        "      200:\n" +
        "        description: 'Some doc'\n" +
        "        body:\n" +
        "          application/json:\n" +
        "            type: Person[]\n" +
        "      404:\n" +
        "        description: 'Not found'", raml);
  }

  Param path(final String name, final Type type) {
    return param(name, RouteParamType.PATH, type);
  }

  Param body(final String name, final Type type) {
    return param(name, RouteParamType.BODY, type);
  }

  Param file(final String name) {
    return param(name, RouteParamType.FILE, Upload.class);
  }

  Param query(final String name, final Type type) {
    return param(name, RouteParamType.QUERY, type);
  }

  Param path(final String name, final Type type, final String doc) {
    return param(name, RouteParamType.PATH, type, doc);
  }

  Param param(final String name, final RouteParamType paramType, final Type type) {
    return param(name, paramType, type, null);
  }

  Param param(final String name, final RouteParamType paramType, final Type type,
      final String doc) {
    return new Param(name, type, paramType, doc);
  }

  Spec route(final String method, final String pattern, final RouteParam... param) {
    return new Spec().method(method).pattern(pattern).params(param);
  }

  Spec route(final String method, final String pattern, final String doc,
      final RouteParam... param) {
    return new Spec().method(method).pattern(pattern).doc(doc).params(param);
  }
}
