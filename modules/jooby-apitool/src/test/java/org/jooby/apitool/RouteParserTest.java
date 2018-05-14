package org.jooby.apitool;

import apps.AppWithDoc;
import apps.Pet;
import com.google.common.collect.ImmutableMap;
import com.google.inject.util.Types;
import static com.google.inject.util.Types.listOf;
import static com.google.inject.util.Types.mapOf;
import static com.google.inject.util.Types.newParameterizedType;
import org.jooby.Jooby;
import org.jooby.Result;
import org.junit.Test;
import parser.CompApp;
import parser.ComplexApp;
import parser.EdgeApp;
import parser.ExtR;
import parser.FileApp;
import parser.Foo;
import parser.FormAndBodyApp;
import parser.GenFoo;
import parser.HeadersApp;
import parser.HelloWorld;
import parser.MethodRefApp;
import parser.MvcApp;
import parser.ParamsApp;
import parser.UsePathApp;
import parser.UseRouteApp;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

public class RouteParserTest {

  @Test
  public void fileApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new FileApp()))
        .next(r -> {
          r.method("POST");
          r.pattern("/");
          r.returnType(String.class);
          r.param(p -> {
            p.name("f1")
                .type(File.class)
                .value(null)
                .kind(RouteParameter.Kind.FILE);
          }).param(p -> {
            p.name("files")
                .type(listOf(File.class))
                .value(null)
                .kind(RouteParameter.Kind.FILE);
          });
        })
        .done();
  }

  @Test
  public void apiLike() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new AppWithDoc()))
        .next(r -> {
          r.method("GET");
          r.pattern("/");
          r.summary(null);
          r.description("Home page.");
          r.returnType(String.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/api/pets/{id}");
          r.returnType(Pet.class);
          r.summary("Everything about your Pets.");
          r.description("Find pet by ID.");
          r.returns(
              "Returns <code>200</code> with a single pet or <code>404</code>");
          r.param(p -> {
            p.name("id")
                .description("Pet ID.")
                .type(int.class);
          });
          r.status(200, "Success");
          r.status(404, "Not Found");
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/api/pets");
          r.returnType(listOf(Pet.class));
          r.param(p -> {
            p.name("start")
                .type(int.class)
                .value(0);
          }).param(p -> {
            p.name("max")
                .type(int.class)
                .value(200);
          });
        })
        .next(r -> {
          r.method("POST");
          r.pattern("/api/pets");
          r.returnType(Pet.class);
          r.param(p -> {
            p.name("body")
                .type(Pet.class)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.method("DELETE");
          r.pattern("/api/pets/{id}");
          r.returnType(Pet.class);
          r.param(p -> {
            p.name("id")
                .type(int.class);
          });
        })
        .done();
  }

  @Test
  public void edgeApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new EdgeApp()))
        .next(r -> {
          r.method("*");
          r.pattern("/**");
          r.description("Edge comment 1");
          r.returnType(void.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("*");
          r.pattern("/**");
          r.description("Edge comment 2");
          r.returnType(void.class);
          r.hasNoParameters();
        })
        .done();
  }

  @Test
  public void useRouteApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new UseRouteApp()))
        .next(r -> {
          r.method("*");
          r.pattern("/api/path");
          r.returnType(String.class);
          r.param(p -> {
            p.name("wild")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .done();
  }

  @Test
  public void usePathApp() throws Throwable {
//    new RouteMethodAssert(new ApiParser(dir()).parseFully(new UsePathApp()))
//        .next(r -> {
//          r.method("GET");
//          r.pattern("/api/path");
//          r.returnType(listOf(Foo.class));
//          r.hasNoParameters();
//        })
//        .next(r -> {
//          r.method("GET");
//          r.pattern("/api/path/{id}");
//          r.returnType(Foo.class);
//          r.param(p -> {
//            p.name("id")
//                .type(int.class)
//                .value(null)
//                .kind(RouteParameter.Kind.PATH);
//          });
//        })
//        .done();

    new RouteMethodAssert(new ApiParser(dir()).parseFully(kt("kt.UsePathApp")))
        .next(r -> {
          r.method("GET");
          r.summary("Summary API.");
          r.description("List all.");
          r.pattern("/api/path");
          r.returnType(listOf(Foo.class));
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/api/path/{id}");
          r.returnType(Foo.class);
          r.summary("Summary API.");
          r.description("List one.");
          r.returns("Foo.");
          r.param(p -> {
            p.name("id")
                .type(int.class)
                .value(null)
                .kind(RouteParameter.Kind.PATH)
                .description("ID.");
          });
        })
        .done();
  }

  @Test
  public void kotlinCompApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(kt("kt.CompApp")))
        .next(r -> {
          r.method("GET");
          r.pattern("/r1");
          r.returnType(boolean.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/c1");
          r.returnType(int.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/c2");
          r.returnType(boolean.class);
          r.hasNoParameters();
        })
        .done();
  }

  @Test
  public void kotlinJavaApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(kt("kt.JavaApp")))
        .next(r -> {
          r.method("PUT");
          r.pattern("/java");
          r.returnType(String.class);
          r.param(p -> {
            p.name("p1")
                .type(String.class);
          });
        }).done();
  }

  @Test
  public void kotlinApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(kt("kt.HelloWorld")))
        .next(r -> {
          r.method("GET");
          r.pattern("/");
          r.returnType(String.class);
          r.description("Get with default arg");
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/kstr");
          r.returnType(String.class);
          r.description("Get on /kstr");
          r.param(p -> {
            p.name("foo")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description("Foo param.");
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/int");
          r.returnType(int.class);
          r.param(p -> {
            p.name("ivar")
                .type(int.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/foo");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/java");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/defpara");
          r.returnType(String.class);
          r.param(p -> {
            p.name("foo")
                .type(String.class)
                .value("bar")
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/list");
          r.returnType(listOf(String.class));
          r.description("List of a, b,c");
          r.returns("[a, b, c]");
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/data1");
          r.returnType("kt.MyData");
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("POST");
          r.pattern("/data2");
          r.returnType("kt.MyData");
          r.param(p -> {
            p.name("body")
                .type("kt.MyData")
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .done();
  }

  private Jooby kt(final String classname) throws Exception {
    return (Jooby) getClass().getClassLoader().loadClass(classname).newInstance();
  }

  @Test
  public void complexApp() throws Throwable {
    try {
      new RouteMethodAssert(new ApiParser(dir()).parseFully(new ComplexApp()))
          .next(r -> {
            r.method("GET");
            r.pattern("/c1");
            r.returnType(mapOf(String.class, Integer.class));
            r.hasNoParameters();
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/c1");
            r.returnType(char.class);
            r.param(p -> {
              p.name("c1")
                  .type(char.class)
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            });
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/m1");
            r.returnType(byte.class);
            r.param(p -> {
              p.name("m1")
                  .type(byte.class)
                  .value(33)
                  .kind(RouteParameter.Kind.QUERY);
            });
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/comp2/c2");
            r.returnType(char.class);
            r.param(p -> {
              p.name("c2")
                  .type(char.class)
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            });
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/c2");
            r.returnType(listOf(Foo.class));
            r.param(p -> {
              p.type(listOf(Foo.class))
                  .name("l")
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            });
          })
          .next(r -> {
            r.method("POST");
            r.pattern("/mvc");
            r.returnType(String.class);
            r.param(p -> {
              p.name("q")
                  .type(String.class)
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            }).param(p -> {
              p.name("offset")
                  .type(int.class)
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            }).param(p -> {
              p.name("Max")
                  .type(int.class)
                  .value(null)
                  .kind(RouteParameter.Kind.QUERY);
            }).param(p -> {
              p.name("ID")
                  .type(String.class)
                  .value(null)
                  .kind(RouteParameter.Kind.HEADER);
            }).param(p -> {
              p.name("body")
                  .type(Foo.class)
                  .value(null)
                  .kind(RouteParameter.Kind.BODY);
            });
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/c3");
            r.returnType(listOf(String.class));
            r.param(p -> {
              p.name("foo")
                  .type(String.class)
                  .value("bar")
                  .kind(RouteParameter.Kind.QUERY);
            });
          })
          .next(r -> {
            r.method("GET");
            r.pattern("/4");
            r.returnType(String.class);
            r.hasNoParameters();
          })
          .done();
    } catch (Throwable x) {
      x.printStackTrace();
    }
  }

  @Test
  public void mvcApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new MvcApp()))
        .next(r -> {
          r.method("POST");
          r.pattern("/mvc");
          r.summary("MVC API.");
          r.description("MVC doIt.");
          r.returnType(String.class);
          r.returns("Sterinv value.");
          r.param(p -> {
            p.name("q")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description("Query string. Like: <code>q=foo</code>");
          }).param(p -> {
            p.name("offset")
                .type(int.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description(null);
          }).param(p -> {
            p.name("Max")
                .type(int.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description(null);
          }).param(p -> {
            p.name("ID")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.HEADER)
                .description(null);
          }).param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .done();
  }

  @Test
  public void modularApp() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new CompApp()))
        .next(r -> {
          r.method("GET");
          r.pattern("/c1");
          r.returnType(char.class);
          r.description("Comp1 doc.");
          r.returns("Char value.");
          r.param(p -> {
            p.name("c1")
                .type(char.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .description("Char value.");
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/m1");
          r.returnType(byte.class);
          r.description("M1 API.");
          r.returns("Number 33.");
          r.param(p -> {
            p.name("m1")
                .type(byte.class)
                .value(33)
                .kind(RouteParameter.Kind.QUERY)
                .description("M1 argument.");
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/comp2/c2");
          r.returnType(char.class);
          r.param(p -> {
            p.name("c2")
                .type(char.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .done();
  }

  @Test
  public void methodReference() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new MethodRefApp()))
        .next(r -> {
          r.returnType(listOf(String.class));
          r.param(p -> {
            r.method("GET");
            r.pattern("/e1");
            p.name("foo")
                .type(String.class)
                .value("bar")
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/l1");
          r.returnType(Result.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/e2");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .done();
  }

  @Test
  public void body() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new FormAndBodyApp()))
        .next(r -> {
          r.method("POST");
          r.pattern("/f1");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.method("POST");
          r.pattern("/f2");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("form")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.FORM);
          });
        })
        .next(r -> {
          r.method("POST");
          r.pattern("/f3");
          r.returnType(Foo.class);
          r.param(p -> {
            p.name("body")
                .type(Foo.class)
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .next(r -> {
          r.method("POST");
          r.pattern("/f4");
          r.returnType(listOf(Foo.class));
          r.param(p -> {
            p.name("body")
                .type(listOf(Foo.class))
                .value(null)
                .kind(RouteParameter.Kind.BODY);
          });
        })
        .done();
  }

  ;

  @Test
  public void parse() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new HelloWorld()))
        .next(r -> {
          r.method("GET");
          r.pattern("/map-level");
          r.returnType(mapOf(String.class, Foo.class));
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/two-level");
          r.returnType(
              newParameterizedType(GenFoo.class, newParameterizedType(GenFoo.class, Foo.class)));
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/");
          r.returnType(int.class);
          r.param(p -> {
            p.name("foo")
                .type(String.class)
                .value("x");
          }).param(p -> {
            p.name("bar")
                .type(String.class)
                .value("y");
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/primitives-param");
          r.returnType(int.class);
          r.param(p -> {
            p.name("bool")
                .type(boolean.class)
                .value(true)
                .isOptional();
          }).param(p -> {
            p.name("c")
                .type(char.class)
                .value(null);
          }).param(p -> {
            p.name("b")
                .type(byte.class)
                .value(null);
          }).param(p -> {
            p.name("s")
                .type(short.class)
                .value(null);
          }).param(p -> {
            p.name("i")
                .type(int.class)
                .value(1);
          }).param(p -> {
            p.name("l")
                .type(long.class)
                .value(0L);
          }).param(p -> {
            p.name("f")
                .type(float.class)
                .value(null);
          }).param(p -> {
            p.name("d")
                .type(double.class)
                .value(0.54d)
                .isOptional();
          }).param(p -> {
            p.name("str")
                .type(String.class)
                .value(null);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/static-str");
          r.returnType(String.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/object-type");
          r.returnType(ExtR.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/gen-type");
          r.returnType(newParameterizedType(GenFoo.class, Foo.class));
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/inline-map");
          r.returnType(ImmutableMap.class);
          r.hasNoParameters();
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/api");
          r.returnType(Types.listOf(String.class));
          r.param(p -> {
            p.name("x")
                .type(int.class)
                .value(null);
          }).param(p -> {
            p.name("count")
                .type(int.class)
                .value(null);
          });
        })
        .done();
  }

  @Test
  public void params() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new ParamsApp()))
        .next(r -> {
          r.method("GET");
          r.pattern("/r1");
          r.returnType(String.class);
          r.param(p -> {
            p.name("r1")
                .type(String.class)
                .value("REF_VAL")
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r2");
          r.returnType(int.class);
          r.param(p -> {
            p.name("offset")
                .type(int.class)
                .value(0)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("max")
                .type(int.class)
                .value(50)
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r3");
          r.returnType(long.class);
          r.param(p -> {
            p.name("offset")
                .type(long.class)
                .value(0L)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("max")
                .type(long.class)
                .value(200L)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("max2")
                .type(newParameterizedType(Optional.class, Long.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY)
                .isOptional();
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r4");
          r.returnType(boolean.class);
          r.param(p -> {
            p.name("t")
                .type(boolean.class)
                .value(true)
                .isOptional();
          }).param(p -> {
            p.name("f")
                .type(boolean.class)
                .value(false)
                .isOptional();
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r5");
          r.returnType(String.class);
          r.param(p -> {
            p.name("o1")
                .type(newParameterizedType(Optional.class, String.class))
                .value(null)
                .isOptional();
          }).param(p -> {
            p.name("o2")
                .type(newParameterizedType(Optional.class, Integer.class))
                .value(null)
                .isOptional();
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r6");
          r.returnType(String.class);
          r.param(p -> {
            p.name("e1")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("l1")
                .type(newParameterizedType(List.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("l2")
                .type(newParameterizedType(List.class, Integer.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("s1")
                .type(newParameterizedType(Set.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          }).param(p -> {
            p.name("s2")
                .type(newParameterizedType(SortedSet.class, Long.class))
                .value(null)
                .kind(RouteParameter.Kind.QUERY);
          });
        })
        .next(r -> {
          r.method("GET");
          r.pattern("/r7");
          r.returnType(String.class);
          r.param(p -> {
            p.name("enum1")
                .type(ParamsApp.Letter.class)
                .value(null);
          }).param(p -> {
            p.name("enum2")
                .type(ParamsApp.Letter.class)
                .value("A");
          });
        })
        .done();
  }

  @Test
  public void headers() throws Throwable {
    new RouteMethodAssert(new ApiParser(dir()).parseFully(new HeadersApp()))
        .next(r -> {
          r.returnType(String.class);
          r.param(p -> {
            p.name("r1")
                .type(String.class)
                .value("REF_VAL")
                .kind(RouteParameter.Kind.HEADER);
          });
        })
        .next(r -> {
          r.returnType(int.class);
          r.param(p -> {
            p.name("offset")
                .type(int.class)
                .value(0)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("max")
                .type(int.class)
                .value(50)
                .kind(RouteParameter.Kind.HEADER);
          });
        })
        .next(r -> {
          r.returnType(long.class);
          r.param(p -> {
            p.name("offset")
                .type(long.class)
                .value(0L)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("max")
                .type(long.class)
                .value(200L)
                .kind(RouteParameter.Kind.HEADER);
          });
        })
        .next(r -> {
          r.returnType(boolean.class);

          r.param(p -> {
            p.name("t")
                .type(boolean.class)
                .value(true)
                .kind(RouteParameter.Kind.HEADER)
                .isOptional();
          }).param(p -> {
            p.name("f")
                .type(boolean.class)
                .value(false)
                .kind(RouteParameter.Kind.HEADER)
                .isOptional();
          });
        })
        .next(r -> {
          r.returnType(String.class);

          r.param(p -> {
            p.name("o1")
                .type(newParameterizedType(Optional.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER)
                .isOptional();
          }).param(p -> {
            p.name("o2")
                .type(newParameterizedType(Optional.class, Integer.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER)
                .isOptional();
          });
        })
        .next(r -> {
          r.returnType(String.class);

          r.param(p -> {
            p.name("e1")
                .type(String.class)
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("l1")
                .type(newParameterizedType(List.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("l2")
                .type(newParameterizedType(List.class, Integer.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("s1")
                .type(newParameterizedType(Set.class, String.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("s2")
                .type(newParameterizedType(SortedSet.class, Long.class))
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          });
        })
        .next(r -> {
          r.returnType(String.class);
          r.param(p -> {
            p.name("enum1")
                .type(HeadersApp.Letter.class)
                .value(null)
                .kind(RouteParameter.Kind.HEADER);
          }).param(p -> {
            p.name("enum2")
                .type(HeadersApp.Letter.class)
                .value("A")
                .kind(RouteParameter.Kind.HEADER);
          });
        })
        .done();
  }

  private Path dir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    if (!userdir.toString().endsWith("jooby-apitool")) {
      userdir = userdir.resolve("modules").resolve("jooby-apitool");
    }
    return userdir;
  }
}
