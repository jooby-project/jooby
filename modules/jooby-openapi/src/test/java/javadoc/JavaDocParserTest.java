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
import io.jooby.internal.openapi.javadoc.JavaDocContext;
import io.jooby.internal.openapi.javadoc.JavaDocParser;
import io.jooby.internal.openapi.javadoc.MethodDoc;

public class JavaDocParserTest {

  @Test
  public void apiDoc() throws Exception {
    withDoc(
        Paths.get("javadoc", "input", "ApiDoc.java"),
        doc -> {
          assertEquals("ApiDoc", doc.getSimpleName());
          assertEquals("javadoc.input.ApiDoc", doc.getName());
          assertEquals("Api summary.", doc.getSummary());
          assertEquals(
              "Proin sit amet lectus interdum, porta libero quis, fringilla metus. Integer viverra"
                  + " ante id vestibulum congue. Nam et tortor at magna tempor congue.",
              doc.getDescription());

          withMethod(
              doc,
              "hello",
              List.of("List", "int", "List", "String"),
              method -> {
                assertEquals("This is the Hello /endpoint.", method.getText());
                assertEquals("Person name.", method.getParameterDoc("name"));
                assertEquals("Person age.", method.getParameterDoc("age"));
                assertEquals("This line has a break.", method.getParameterDoc("list"));
                assertEquals("Some string.", method.getParameterDoc("str"));
                assertEquals("Welcome message 200.", method.getReturnDoc());
              });

          withMethod(
              doc,
              "search",
              List.of("QueryBeanDoc"),
              method -> {
                assertEquals("Search database.", method.getText());
                assertEquals(
                    "Filter query. Works like internal filter.",
                    method.getParameterDoc("fq", "javadoc.input.QueryBeanDoc"));
                assertEquals(
                    "Offset, used for paging.",
                    method.getParameterDoc("offset", "javadoc.input.QueryBeanDoc"));
                assertNull(method.getParameterDoc("limit", "javadoc.input.QueryBeanDoc"));
                assertNull(method.getReturnDoc());
              });

          withMethod(
              doc,
              "recordBean",
              List.of("RecordBeanDoc"),
              method -> {
                assertEquals("Record database.", method.getText());
                assertEquals(
                    "Person id.", method.getParameterDoc("id", "javadoc.input.RecordBeanDoc"));
                assertEquals(
                    "Person name. Example: edgar.",
                    method.getParameterDoc("name", "javadoc.input.RecordBeanDoc"));
              });

          withMethod(
              doc,
              "enumParam",
              List.of("EnumDoc"),
              method -> {
                assertEquals("Enum database.", method.getText());
                assertEquals("Enum doc.", method.getParameterDoc("query"));
              });
        });
  }

  @Test
  public void noDoc() throws Exception {
    var result = newParser().parse(Paths.get("javadoc", "input", "NoDoc.java"));
    assertTrue(result.isEmpty());
  }

  private JavaDocParser newParser() {
    return new JavaDocParser(new JavaDocContext(baseDir()));
  }

  private Path baseDir() {
    return Paths.get(System.getProperty("user.dir")).resolve("src").resolve("test").resolve("java");
  }

  private void withDoc(Path path, Consumer<ClassDoc> consumer) throws Exception {
    try {
      var result = newParser().parse(path);
      assertFalse(result.isEmpty());
      consumer.accept(result.get());
    } catch (Throwable cause) {
      var stringAst =
          AstTreeStringPrinter.printFileAst(
              baseDir().resolve(path).toFile(), JavaParser.Options.WITH_COMMENTS);
      cause.addSuppressed(new RuntimeException("\n" + stringAst));
      throw SneakyThrows.propagate(cause);
    }
  }

  private void withMethod(
      ClassDoc doc, String name, List<String> types, Consumer<MethodDoc> consumer) {
    var method = doc.getMethod(name, types);
    assertTrue(method.isPresent());
    consumer.accept(method.get());
  }
}
