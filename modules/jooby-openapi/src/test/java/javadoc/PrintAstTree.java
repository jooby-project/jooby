/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javadoc.input.Subclass;

import com.puppycrawl.tools.checkstyle.AstTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class PrintAstTree {
  public static void main(String[] args) throws CheckstyleException, IOException {
    var baseDir =
        Paths.get(System.getProperty("user.dir"))
            .resolve("modules")
            .resolve("jooby-openapi")
            .resolve("src")
            .resolve("test")
            .resolve("java");
    var stringAst =
        AstTreeStringPrinter.printJavaAndJavadocTree(
            baseDir.resolve(toPath(Subclass.class)).toFile());
    System.out.println(stringAst);
  }

  private static Path toPath(Class<?> typeName) {
    return toPath(typeName.getName());
  }

  private static Path toPath(String typeName) {
    var segments = typeName.split("\\.");
    segments[segments.length - 1] = segments[segments.length - 1] + ".java";
    return Paths.get(String.join(File.separator, segments));
  }
}
