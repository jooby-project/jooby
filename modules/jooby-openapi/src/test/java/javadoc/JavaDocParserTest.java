/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.AstTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavaParser;
import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.javadoc.ClassDoc;
import io.jooby.internal.openapi.javadoc.JavaDocParser;

public class JavaDocParserTest {

  @Test
  public void apiDoc() throws Exception {
    withDoc(
        baseDir().resolve("ApiDoc.java"),
        doc -> {
          assertEquals("ApiDoc", doc.getSimpleName());
          assertEquals("javadoc.input.ApiDoc", doc.getName());
          assertEquals("Api summary.", doc.getSummary());
          assertEquals(
              "Proin sit amet lectus interdum, porta libero quis, fringilla metus. Integer viverra"
                  + " ante id vestibulum congue. Nam et tortor at magna tempor congue.",
              doc.getDescription());
          // throw new UnsupportedOperationException();
          var methods = doc.getMethods();
          assertEquals(2, methods.size());
          assertEquals("hello", methods.get(0).getName());
          assertEquals(List.of("name", "age", "list", "str"), methods.get(0).getParameterNames());
          assertEquals(
              List.of("List", "int", "List", "String"), methods.get(0).getParameterTypes());
          //
          var method = doc.getMethod("hello", List.of("List", "int", "List", "String"));
          assertTrue(method.isPresent());
          assertEquals("This is the Hello /endpoint.", method.get().getText());
          assertEquals("Person name.", method.get().getParameterDoc("name"));
          assertEquals("Person age.", method.get().getParameterDoc("age"));
          assertEquals("This line has a break.", method.get().getParameterDoc("list"));
          assertEquals("Some string.", method.get().getParameterDoc("str"));

          // TODO: continue here
          var search = doc.getMethod("search", List.of("QueryBeanDoc"));
          assertTrue(search.isPresent());
          assertEquals("This is the Hello /endpoint.", search.get().getText());
        });
  }

  @Test
  public void noDoc() throws Exception {
    var result = JavaDocParser.parse(baseDir().resolve("NoDoc.java"));
    assertTrue(result.isEmpty());
  }

  private Path baseDir() {
    var baseDir = Paths.get(System.getProperty("user.dir"));
    return baseDir
        .resolve("src")
        .resolve("test")
        .resolve("java")
        .resolve("javadoc")
        .resolve("input");
  }

  private void withDoc(Path path, Consumer<ClassDoc> consumer) throws Exception {
    try {
      var result = JavaDocParser.parse(path);
      assertFalse(result.isEmpty());
      consumer.accept(result.get());
    } catch (Throwable cause) {
      var stringAst =
          AstTreeStringPrinter.printFileAst(path.toFile(), JavaParser.Options.WITH_COMMENTS);
      cause.addSuppressed(new RuntimeException("\n" + stringAst));
      throw SneakyThrows.propagate(cause);
    }
  }
}
