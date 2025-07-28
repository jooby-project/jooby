/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import java.io.IOException;
import java.nio.file.Paths;

import com.puppycrawl.tools.checkstyle.AstTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class PrinteAstTree {
  public static void main(String[] args) throws CheckstyleException, IOException {
    var baseDir =
        Paths.get(System.getProperty("user.dir")).resolve("modules").resolve("jooby-openapi");
    var input = Paths.get("src", "test", "java", "javadoc", "input", "EnumDoc.java");
    var stringAst = AstTreeStringPrinter.printJavaAndJavadocTree(baseDir.resolve(input).toFile());
    System.out.println(stringAst);
  }
}
