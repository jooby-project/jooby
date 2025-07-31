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
import javadoc.input.EnumDoc;

import org.junit.jupiter.api.Test;

import io.jooby.SneakyThrows;
import io.jooby.internal.openapi.javadoc.ClassDoc;
import io.jooby.internal.openapi.javadoc.FieldDoc;
import io.jooby.internal.openapi.javadoc.JavaDocParser;
import io.jooby.internal.openapi.javadoc.MethodDoc;
import issues.i3729.api.Book;

public class JavaDocParserTest {

  @Test
  public void apiDoc() throws Exception {
    withDoc(
        javadoc.input.ApiDoc.class,
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
              List.of("name", "age", "list", "str"),
              method -> {
                assertEquals("This is the Hello /endpoint", method.getSummary());
                assertEquals("Operation description", method.getDescription());
                assertEquals("Person name.", method.getParameterDoc("name"));
                assertEquals("Person age.", method.getParameterDoc("age"));
                assertEquals("This line has a break.", method.getParameterDoc("list"));
                assertEquals("Some string.", method.getParameterDoc("str"));
                assertEquals("Welcome message 200.", method.getReturnDoc());
              });

          withMethod(
              doc,
              "search",
              List.of("query"),
              method -> {
                assertEquals("Search database.", method.getSummary());
                assertEquals("Search DB", method.getDescription());
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
              List.of("query"),
              method -> {
                assertEquals("Record database.", method.getSummary());
                assertNull(method.getDescription());
                assertEquals(
                    "Person id. Unique person identifier.",
                    method.getParameterDoc("id", "javadoc.input.RecordBeanDoc"));
                assertEquals(
                    "Person name. Example: edgar.",
                    method.getParameterDoc("name", "javadoc.input.RecordBeanDoc"));
              });

          withMethod(
              doc,
              "enumParam",
              List.of("query"),
              method -> {
                assertEquals("Enum database.", method.getSummary());
                assertEquals("Enum doc.", method.getParameterDoc("query"));
              });
        });
  }

  @Test
  public void ignoreStatementComment() throws Exception {
    var result = newParser().parse("issues.i1580.Controller1580");
    assertTrue(result.isEmpty());
  }

  @Test
  public void noDoc() throws Exception {
    var result = newParser().parse("javadoc.input.NoDoc");
    assertTrue(result.isEmpty());
  }

  @Test
  public void noClassDoc() throws Exception {
    withDoc(
        javadoc.input.NoClassDoc.class,
        doc -> {
          assertNull(doc.getSummary());
          assertNull(doc.getDescription());

          withMethod(
              doc,
              "hello",
              List.of("name"),
              methodDoc -> {
                assertEquals("Method Doc.", methodDoc.getSummary());
                assertNull(methodDoc.getDescription());
              });
        });
  }

  @Test
  public void shouldParseEnum() throws Exception {
    withDoc(
        EnumDoc.class,
        doc -> {
          assertEquals("Enum summary.", doc.getSummary());
          assertEquals("Enum desc.", doc.getDescription());
          assertEquals(
              "Enum summary.\n" + "  - Foo: Foo doc.\n" + "  - Bar: Bar doc.", doc.getText());
        });
  }

  @Test
  public void shouldParseBean() throws Exception {
    withDoc(
        Book.class,
        doc -> {
          assertEquals("Book model.", doc.getSummary());
          assertNull(doc.getDescription());

          // bean like
          assertEquals("Book's title.", doc.getPropertyDoc("title"));
        });

    withDoc(
        javadoc.input.QueryBeanDoc.class,
        doc -> {
          assertEquals("Search options.", doc.getSummary());
          assertNull(doc.getDescription());

          withMethod(
              doc,
              "getFq",
              List.of(),
              methodDoc -> {
                assertEquals("Filter query.", methodDoc.getSummary());
                assertEquals("Works like internal filter.", methodDoc.getDescription());
              });

          // bean like
          assertEquals("Filter query. Works like internal filter.", doc.getPropertyDoc("fq"));
          withField(
              doc,
              "fq",
              field -> {
                assertEquals("The field comment.", field.getSummary());
              });
          assertEquals("Offset, used for paging.", doc.getPropertyDoc("offset"));
        });
  }

  @Test
  public void shouldRecord() throws Exception {
    withDoc(
        javadoc.input.RecordBeanDoc.class,
        doc -> {
          assertEquals("Record documentation.", doc.getSummary());
          assertNull(doc.getDescription());

          withMethod(
              doc,
              "id",
              List.of(),
              methodDoc -> {
                assertEquals("Person id.", methodDoc.getSummary());
                assertEquals("Unique person identifier.", methodDoc.getDescription());
              });

          // bean like
          assertEquals("Person id. Unique person identifier.", doc.getPropertyDoc("id"));
          withField(
              doc,
              "id",
              field -> {
                assertEquals("Person id.", field.getSummary());
                assertEquals("Unique person identifier.", field.getDescription());
                ;
              });
        });
  }

  @Test
  public void shouldVerifyJavaDocScope() throws Exception {
    withDoc(
        javadoc.input.ScopeDoc.class,
        doc -> {
          assertEquals("Class", doc.getSummary());
          assertNull(doc.getDescription());

          withMethod(
              doc,
              "getName",
              List.of(),
              methodDoc -> {
                assertEquals("Method", methodDoc.getSummary());
                assertNull(methodDoc.getDescription());
              });

          withField(
              doc,
              "name",
              methodDoc -> {
                assertEquals("Field", methodDoc.getSummary());
                assertNull(methodDoc.getDescription());
              });
        });
  }

  private JavaDocParser newParser() {
    return new JavaDocParser(baseDir());
  }

  private Path baseDir() {
    return Paths.get(System.getProperty("user.dir")).resolve("src").resolve("test").resolve("java");
  }

  private void withDoc(Class<?> typeName, Consumer<ClassDoc> consumer) throws Exception {
    try {
      var result = newParser().parse(typeName.getName());
      assertFalse(result.isEmpty());
      consumer.accept(result.get());
    } catch (Throwable cause) {
      throw SneakyThrows.propagate(cause);
    }
  }

  private void withMethod(
      ClassDoc doc, String name, List<String> types, Consumer<MethodDoc> consumer) {
    var method = doc.getMethod(name, types);
    assertTrue(method.isPresent());
    consumer.accept(method.get());
  }

  private void withField(ClassDoc doc, String name, Consumer<FieldDoc> consumer) {
    var method = doc.getField(name);
    assertTrue(method.isPresent());
    consumer.accept(method.get());
  }
}
