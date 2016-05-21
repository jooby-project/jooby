package org.jooby.spec.issues;

import static org.junit.Assert.assertEquals;

import org.jooby.internal.spec.RouteParamCollector;
import org.jooby.spec.ASTTest;
import org.junit.Test;

import com.github.javaparser.ParseException;

public class Issue378 extends ASTTest {

  @Test
  public void shouldDetectParamInsideExpression() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "Cat cat = new Cat();",
        "cat.setName(req.param(\"name\").value());",
        "}"), ctx()))
            .next(p -> {
              assertEquals("name", p.name());
              assertEquals(String.class, p.type());
              assertEquals(null, p.value());
            });
  }

}
