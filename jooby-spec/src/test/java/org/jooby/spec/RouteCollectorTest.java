package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import org.jooby.internal.spec.AppCollector;
import org.jooby.internal.spec.RouteCollector;
import org.junit.Test;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public class RouteCollectorTest extends ASTTest {

  @Test
  public void helloWorld() throws ParseException {
    CompilationUnit unit = source("package myapp;",
        "import org.jooby.Jooby;",
        "public class App extends Jooby {",
        "  {",
        "    get(\"/\", () -> \"Hello World!\");",
        "  }",
        "}");
    Node app = new AppCollector().accept(unit);

    routes(new RouteCollector().accept(app, ctx()))
        .script((m, l) -> {
          assertEquals("get(\"/\", () -> \"Hello World!\")", m.toString());
          assertEquals("() -> \"Hello World!\"", l.toString());
        });
  }

  @Test
  public void importApp() throws ParseException {
    CompilationUnit unit = source("package myapp;",
        "import org.jooby.Jooby;",
        "import apps.BlogApi;",
        "public class App extends Jooby {",
        "  {",
        "    use(\"/api/blogs\", new BlogApi());",
        "  }",
        "}");
    Node app = new AppCollector().accept(unit);

    routes(new RouteCollector().accept(app, ctx()))
        .script((m, l) -> {
          assertEquals("get(\"/:id\",  req -> {\n" +
              "    int id = req.param(\"id\").intValue();\n" +
              "    DB db = req.require(DB.class);\n" +
              "    Blog result = db.find(id);\n" +
              "    return result;\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    int id = req.param(\"id\").intValue();\n" +
              "    DB db = req.require(DB.class);\n" +
              "    Blog result = db.find(id);\n" +
              "    return result;\n" +
              "}", l.toString());
        });
  }

  @Test
  public void apiLike() throws ParseException {
    CompilationUnit unit = source("package myapp;",
        "import org.jooby.Jooby;",
        "public class App extends Jooby {",
        "  {",
        "    use(\"/api/pets\")",
        "       .get(req -> {",
        "         return \"Aaa\";",
        "       })",
        "       .get(\"/:id\", req -> {",
        "         return \"Bbb\";",
        "       });",
        "  }",
        "}");
    Node app = new AppCollector().accept(unit);

    routes(new RouteCollector().accept(app, ctx()))
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Aaa\";\n" +
              "}", l.toString());
        })
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "}).get(\"/:id\",  req -> {\n" +
              "    return \"Bbb\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Bbb\";\n" +
              "}", l.toString());
        });
  }

  @Test
  public void mixedApp() throws ParseException {
    CompilationUnit unit = source("package myapp;",
        "import org.jooby.Jooby;",
        "import apps.MvcRoutes;",
        "public class App extends Jooby {",
        "  {",
        "    use(\"/api/pets\")",
        "       .get(req -> {",
        "         return \"Aaa\";",
        "       })",
        "       .get(\"/:id\", req -> {",
        "         return \"Bbb\";",
        "       });",
        "",
        "    use(MvcRoutes.class);",
        "    post(\"/\", () -> \"Hello World!\");",
        "  }",
        "}");
    Node app = new AppCollector().accept(unit);

    routes(new RouteCollector().accept(app, ctx()))
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Aaa\";\n" +
              "}", l.toString());
        })
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "}).get(\"/:id\",  req -> {\n" +
              "    return \"Bbb\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Bbb\";\n" +
              "}", l.toString());
        })
        .mvc((m, b) -> {
          assertEquals("@Path(\"/:id\")\n" +
              "@GET\n" +
              "public Pet get(final int id) {\n" +
              "    return null;\n" +
              "}", m.toString());
          assertEquals("{\n" +
              "    return null;\n" +
              "}", b.toString());
        }).mvc((m, b) -> {
          assertEquals("@GET\n" +
              "public List<Pet> list(final Optional<Integer> start, final Optional<Integer> max) {\n" +
              "    return null;\n" +
              "}", m.toString());
          assertEquals("{\n" +
              "    return null;\n" +
              "}", b.toString());
        }).mvc((m, b) -> {
          assertEquals("@POST\n" +
              "public Pet create(@Body final Pet pet) {\n" +
              "    return null;\n" +
              "}", m.toString());
          assertEquals("{\n" +
              "    return null;\n" +
              "}", b.toString());
        }).script((m, l) -> {
          assertEquals("post(\"/\", () -> \"Hello World!\")", m.toString());
          assertEquals("() -> \"Hello World!\"", l.toString());
        });
  }

  @Test
  public void multipleScriptRoutes() throws ParseException {
    CompilationUnit unit = source("package myapp;",
        "import org.jooby.Jooby;",
        "public class App extends Jooby {",
        "  {",
        "    get(\"/\", () -> \"Hello World!\");",
        "    use(\"/api/pets\")",
        "       .get(req -> {",
        "         return \"Aaa\";",
        "       })",
        "       .get(\"/:id\", req -> {",
        "         return \"Bbb\";",
        "       });",
        "    post(\"/\", () -> \"Hello World!\");",
        "  }",
        "}");
    Node app = new AppCollector().accept(unit);
    routes(new RouteCollector().accept(app, ctx()))
        .script((m, l) -> {
          assertEquals("get(\"/\", () -> \"Hello World!\")", m.toString());
          assertEquals("() -> \"Hello World!\"", l.toString());
        })
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Aaa\";\n" +
              "}", l.toString());
        })
        .script((m, l) -> {
          assertEquals("use(\"/api/pets\").get( req -> {\n" +
              "    return \"Aaa\";\n" +
              "}).get(\"/:id\",  req -> {\n" +
              "    return \"Bbb\";\n" +
              "})", m.toString());
          assertEquals(" req -> {\n" +
              "    return \"Bbb\";\n" +
              "}", l.toString());
        })
        .script((m, l) -> {
          assertEquals("post(\"/\", () -> \"Hello World!\")", m.toString());
          assertEquals("() -> \"Hello World!\"", l.toString());
        });
  }

}
