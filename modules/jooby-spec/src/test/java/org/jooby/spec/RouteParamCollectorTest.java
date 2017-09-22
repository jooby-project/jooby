package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Upload;
import org.jooby.internal.spec.RouteParamCollector;
import org.junit.Test;

import com.github.javaparser.ParseException;
import com.google.inject.util.Types;

import apps.Letter;
import apps.model.Pet;

public class RouteParamCollectorTest extends ASTTest {

  @Test
  public void paramCollector() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "char ch = req.param(\"ch\").charValue();",
        "req.param(\"bool\").booleanValue();",
        "req.param(\"sh\").shortValue();",
        "int id = req.param(\"id\").intValue();",
        "req.param(\"l\").longValue();",
        "req.param(\"f\").floatValue();",
        "req.param(\"d\").doubleValue();",
        "req.param(\"v\").value();",
        "}"), ctx()))
            .next(p -> {
              assertEquals("ch", p.name());
              assertEquals(char.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("bool", p.name());
              assertEquals(boolean.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("sh", p.name());
              assertEquals(short.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("id", p.name());
              assertEquals(int.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("l", p.name());
              assertEquals(long.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("f", p.name());
              assertEquals(float.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("d", p.name());
              assertEquals(double.class, p.type());
              assertEquals(null, p.value());
            })
            .next(p -> {
              assertEquals("v", p.name());
              assertEquals(String.class, p.type());
              assertEquals(null, p.value());
            });
  }

  @Test
  public void paramWithDef() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"bool\").booleanValue(true);",
        "req.param(\"str\").value(\"str\");",
        "req.param(\"i\").intValue(678);",
        "req.param(\"ch\").intValue('c');",
        "}"), ctx()))
    .next(p -> {
      assertEquals("bool", p.name());
      assertEquals(boolean.class, p.type());
      assertEquals(true, p.value());
    })
    .next(p -> {
      assertEquals("str", p.name());
      assertEquals(String.class, p.type());
      assertEquals("str", p.value());
    })
    .next(p -> {
      assertEquals("i", p.name());
      assertEquals(int.class, p.type());
      assertEquals(678, p.value());
    })
    .next(p -> {
      assertEquals("ch", p.name());
      assertEquals(int.class, p.type());
      assertEquals('c', p.value());
    });
  };

  @Test
  public void paramToOptional() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"opt\").toOptional();",
        "req.param(\"iopt\").toOptional(Integer.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("opt", p.name());
      assertEquals(Types.newParameterizedType(Optional.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("iopt", p.name());
      assertEquals(Types.newParameterizedType(Optional.class, Integer.class), p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void paramToList() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"p1\").toList();",
        "req.param(\"p2\").toList(Integer.class);",
        "req.param(\"p3\").toList(apps.model.Pet.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(Types.newParameterizedType(List.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Types.newParameterizedType(List.class, Integer.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p3", p.name());
      assertEquals(Types.newParameterizedType(List.class, Pet.class), p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void paramToSet() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"p1\").toSet();",
        "req.param(\"p2\").toSet(String.class);",
        "req.param(\"p3\").toSet(apps.model.Pet.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(Types.newParameterizedType(Set.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Types.newParameterizedType(Set.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p3", p.name());
      assertEquals(Types.newParameterizedType(Set.class, Pet.class), p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void paramToSortedSet() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"p1\").toSortedSet();",
        "req.param(\"p2\").toSortedSet(String.class);",
        "req.param(\"p3\").toSortedSet(apps.model.Pet.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(Types.newParameterizedType(SortedSet.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Types.newParameterizedType(SortedSet.class, String.class), p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p3", p.name());
      assertEquals(Types.newParameterizedType(SortedSet.class, Pet.class), p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void paramToUpload() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.file(\"p1\");",
        "req.files(\"p2\");",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(Upload.class, p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Types.newParameterizedType(List.class, Upload.class), p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void paramToEnum() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"p1\").toEnum(apps.Letter.class);",
        "req.param(\"p2\").toEnum(apps.Letter.A);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(Letter.class, p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Letter.class, p.type());
      assertEquals("A", p.value());
    });
  };

  @Test
  public void paramTo() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.param(\"p1\").to(String.class);",
        "req.param(\"p2\").to(apps.model.Pet.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("p1", p.name());
      assertEquals(String.class, p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("p2", p.name());
      assertEquals(Pet.class, p.type());
      assertEquals(null, p.value());
    });
  };

  @Test
  public void header() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.header(\"h1\").intValue();",
        "}"), ctx()))
    .next(p -> {
      assertEquals("h1", p.name());
      assertEquals(int.class, p.type());
      assertEquals(null, p.value());
      assertEquals(RouteParamType.HEADER, p.paramType());
    });
  };

  @Test
  public void bodyTo() throws ParseException {
    params(new RouteParamCollector().accept(expr("req -> {",
        "req.body().to(apps.model.Pet.class);",
        "req.body().toList(apps.model.Pet.class);",
        "}"), ctx()))
    .next(p -> {
      assertEquals("<body>", p.name());
      assertEquals(Pet.class, p.type());
      assertEquals(null, p.value());
    })
    .next(p -> {
      assertEquals("<body>", p.name());
      assertEquals(Types.newParameterizedType(List.class, Pet.class), p.type());
      assertEquals(null, p.value());
    });
  };

}
