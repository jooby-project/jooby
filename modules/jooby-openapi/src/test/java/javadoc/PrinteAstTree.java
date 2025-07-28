/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import java.io.IOException;
import java.nio.file.Paths;

import com.puppycrawl.tools.checkstyle.AstTreeStringPrinter;
import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

public class PrinteAstTree {
  public static void main(String[] args) throws CheckstyleException, IOException {
    var baseDir =
        Paths.get(System.getProperty("user.dir")).resolve("modules").resolve("jooby-openapi");
    var input = Paths.get("src", "test", "java", "javadoc", "input", "QueryBeanDoc.java");
    var stringAst =
        AstTreeStringPrinter.printFileAst(
            baseDir.resolve(input).toFile(), JavaParser.Options.WITH_COMMENTS);
    System.out.println(stringAst);
  }
}
