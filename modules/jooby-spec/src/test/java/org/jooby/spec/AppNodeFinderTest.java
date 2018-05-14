package org.jooby.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jooby.internal.spec.AppCollector;
import org.junit.Test;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;

public class AppNodeFinderTest extends ASTTest {

  @Test
  public void findAppNode() throws ParseException {
    Node app = new AppCollector().accept(source("package myapp;",
        "import org.jooby.Jooby;",
        "public class App extends Jooby {",
        "  {",
        "    get(\"/\", () -> \"Hello World!\");",
        "  }",
        "}"), ctx());
    assertNotNull(app);
    assertEquals("{\n" +
        "    get(\"/\", () -> \"Hello World!\");\n" +
        "}", app.toString());
  }

}
