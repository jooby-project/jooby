/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.javadoc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodDocTest {

  /**
   * Helper method to reliably extract the Javadoc block from the parsed method node. Returns
   * EMPTY_AST instead of null to prevent Checkstyle parsing exceptions.
   */
  private DetailAST getJavadocNode(DetailAST methodNode) {
    var node = JavadocTestHelper.findToken(methodNode, TokenTypes.BLOCK_COMMENT_BEGIN);
    return node != null ? node : JavaDocNode.EMPTY_AST;
  }

  @Test
  public void shouldExtractSecurityRequirements() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;

        public class Controller {
          /**
           * @securityRequirement myOauth [read:users, write:users]
           */
          public void myMethod() {}
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();
    var javadocNode = getJavadocNode(methodNode);

    var doc = new MethodDoc(null, methodNode, javadocNode);
    var security = doc.getSecurityRequirements();

    assertNotNull(security, "Security requirements should not be null");
    assertEquals(1, security.size());
    assertEquals(List.of("read:users", "write:users"), security.getFirst().get("myOauth"));
  }

  @Test
  public void shouldExtractParameters() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;

        public class Controller {
          /**
           * @param userId The ID of the user.
           * @param filter An optional filter to apply.
           */
          public void myMethod(String userId, String filter) {}
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();
    var javadocNode = getJavadocNode(methodNode);

    var doc = new MethodDoc(null, methodNode, javadocNode);

    assertEquals("The ID of the user.", doc.getParameterDoc("userId"));
    assertEquals("An optional filter to apply.", doc.getParameterDoc("filter"));
    assertNull(doc.getParameterDoc("missingParameter"));
  }

  @Test
  public void shouldExtractReturnDoc() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;
        import java.util.List;

        public class Controller {
          /**
           * @return A list of active users in the system.
           */
          public List<String> myMethod() { return null; }
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();
    var javadocNode = getJavadocNode(methodNode);

    var doc = new MethodDoc(null, methodNode, javadocNode);

    assertEquals("A list of active users in the system.", doc.getReturnDoc());
  }

  @Test
  public void shouldExtractThrowsWithStatusCode() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;

        public class Controller {
          /**
           * @throws IllegalArgumentException <code>400</code> if the input is invalid.
           * @throws IllegalStateException <code>409</code> Conflict occurred.
           */
          public void myMethod() {}
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();
    var javadocNode = getJavadocNode(methodNode);

    var doc = new MethodDoc(null, methodNode, javadocNode);
    var throwsMap = doc.getThrows();

    assertNotNull(throwsMap);
    assertEquals(2, throwsMap.size());
    // The parser concatenates the StatusCode reason with the description text
    assertEquals("Bad Request: if the input is invalid.", throwsMap.get(400));
    assertEquals("Conflict: Conflict occurred.", throwsMap.get(409));
  }

  @Test
  public void shouldExtractOperationId() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;

        public class Controller {
          /**
           * @operationId customOperationName
           */
          public void myMethod() {}
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();
    var javadocNode = getJavadocNode(methodNode);

    var doc = new MethodDoc(null, methodNode, javadocNode);

    assertEquals("customOperationName", doc.getOperationId());
  }

  @Test
  public void shouldHandleMethodsWithoutJavadocGracefully() throws Exception {
    var source =
        """
        package io.jooby.internal.javadoc;

        public class Controller {
          public void myMethod() {}
        }
        """;

    var result = JavadocTestHelper.parseCode(source);
    var methodNode = result.getMethodNode();

    // javadocNode will be null because there is no block comment
    var javadocNode = getJavadocNode(methodNode);

    // Our null-safe toJavaDocNode() fix handles this gracefully
    var doc = new MethodDoc(null, methodNode, javadocNode);

    assertNull(doc.getOperationId());
    assertNull(doc.getReturnDoc());
    assertTrue(doc.getSecurityRequirements().isEmpty());
    assertTrue(doc.getThrows().isEmpty());
  }
}
