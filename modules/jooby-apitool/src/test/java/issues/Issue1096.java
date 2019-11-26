package issues;

import apps.App1096;
import apps.App1096b;
import apps.App1096c;
import apps.App1096d;
import apps.Form1096;
import apps.Param1096;
import kt.App1096e;
import kt.Query1096;
import org.jooby.apitool.ApiParser;
import org.jooby.apitool.ApiToolFeature;
import org.jooby.apitool.RouteMethod;
import org.jooby.apitool.RouteMethodAssert;
import org.jooby.apitool.RouteParameter;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class Issue1096 extends ApiToolFeature {

  @Test
  public void shouldExpandQueryBean() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1096b());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/1096/search");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type(Param1096.class);
            p.name("params");
            p.kind(RouteParameter.Kind.QUERY);
          });
        })
        .done();

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"1096\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /1096/search:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"1096\"\n"
        + "      operationId: \"get1096ByParams\"\n"
        + "      parameters:\n"
        + "      - name: \"param1\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param2\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      - name: \"param3\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param4\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param5\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"nested.nested1\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }

  @Test
  public void shouldExpandForm() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1096c());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("POST");
          r.pattern("/1096/form");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type(Form1096.class);
            p.name("form");
            p.kind(RouteParameter.Kind.FORM);
          });
        })
        .done();

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"1096\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /1096/form:\n"
        + "    post:\n"
        + "      tags:\n"
        + "      - \"1096\"\n"
        + "      operationId: \"post1096\"\n"
        + "      consumes:\n"
        + "      - \"application/x-www-form-urlencoded\"\n"
        + "      - \"multipart/form-data\"\n"
        + "      parameters:\n"
        + "      - name: \"param1\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param2\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      - name: \"param3\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param4\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param5\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"nested.nested1\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"file\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"file\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }

  @Test
  public void shouldExpandFormMvc() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1096d());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("POST");
          r.pattern("/1096/mvc/form");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type(Form1096.class);
            p.name("form");
            p.kind(RouteParameter.Kind.FORM);
          });
        })
        .done();

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"Route1096d\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /1096/mvc/form:\n"
        + "    post:\n"
        + "      tags:\n"
        + "      - \"Route1096d\"\n"
        + "      operationId: \"/Route1096d.myApi\"\n"
        + "      consumes:\n"
        + "      - \"application/x-www-form-urlencoded\"\n"
        + "      - \"multipart/form-data\"\n"
        + "      parameters:\n"
        + "      - name: \"param1\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param2\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      - name: \"param3\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param4\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param5\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"nested.nested1\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"file\"\n"
        + "        in: \"formData\"\n"
        + "        required: false\n"
        + "        type: \"file\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }

  @Test
  public void shouldExpandQueryBeanFromMvc() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1096());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type(Param1096.class);
            p.name("params");
            p.kind(RouteParameter.Kind.QUERY);
          });
        })
        .done();

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"Route1096\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"Route1096\"\n"
        + "      operationId: \"/Route1096.myApi\"\n"
        + "      parameters:\n"
        + "      - name: \"param1\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param2\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"integer\"\n"
        + "        format: \"int32\"\n"
        + "      - name: \"param3\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param4\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"param5\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"nested.nested1\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }

  @Test
  public void shouldExpandQueryBeanFromKotlin() throws Exception {
    List<RouteMethod> routes = new ApiParser(dir()).parseFully(new App1096e());

    new RouteMethodAssert(routes)
        .next(r -> {
          r.returnType(String.class);
          r.method("GET");
          r.pattern("/1096/kt");
          r.description(null);
          r.summary(null);
          r.returns(null);
          r.param(p -> {
            p.type(Query1096.class);
            p.name("params");
            p.kind(RouteParameter.Kind.QUERY);
          });
        })
        .done();

    assertEquals("---\n"
        + "swagger: \"2.0\"\n"
        + "tags:\n"
        + "- name: \"1096Kt\"\n"
        + "consumes:\n"
        + "- \"application/json\"\n"
        + "produces:\n"
        + "- \"application/json\"\n"
        + "paths:\n"
        + "  /1096/kt:\n"
        + "    get:\n"
        + "      tags:\n"
        + "      - \"1096Kt\"\n"
        + "      operationId: \"get1096KtByParams\"\n"
        + "      parameters:\n"
        + "      - name: \"name\"\n"
        + "        in: \"query\"\n"
        + "        required: true\n"
        + "        type: \"string\"\n"
        + "      - name: \"firstname\"\n"
        + "        in: \"query\"\n"
        + "        required: false\n"
        + "        type: \"string\"\n"
        + "      - name: \"picture.url\"\n"
        + "        in: \"query\"\n"
        + "        required: true\n"
        + "        type: \"string\"\n"
        + "      responses:\n"
        + "        \"200\":\n"
        + "          description: \"String\"\n"
        + "          schema:\n"
        + "            type: \"string\"\n", yaml(swagger(routes)));
  }
}
