package io.jooby;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UrlParserTest {

  @Test
  public void simpleQueryString() {
    queryString("?&foo=bar", queryString -> {
      assertEquals("?&foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?foo=bar&", queryString -> {
      assertEquals("?foo=bar&", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?foo=bar&&", queryString -> {
      assertEquals("?foo=bar&&", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });

    queryString("?foo=bar", queryString -> {
      assertEquals("?foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?foo=bar", queryString -> {
      assertEquals("?foo=bar", queryString.queryString());
      assertEquals("bar", queryString.get("foo").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?a=1&b=2", queryString -> {
      assertEquals("?a=1&b=2", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("/path?a=1&b=2&", queryString -> {
      assertEquals("?a=1&b=2&", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("/path?a=1&&b=2&", queryString -> {
      assertEquals("?a=1&&b=2&", queryString.queryString());
      assertEquals(1, queryString.get("a").intValue());
      assertEquals(2, queryString.get("b").intValue());
      assertEquals(2, queryString.size());
    });
    queryString("/path?a=1&a=2", queryString -> {
      assertEquals("?a=1&a=2", queryString.queryString());
      assertEquals(1, queryString.get("a").get(0).intValue());
      assertEquals(2, queryString.get("a").get(1).intValue());
      assertEquals(2, queryString.get("a").size());
      assertEquals(1, queryString.size());
    });
    queryString("/path?a=1;a=2", queryString -> {
      assertEquals("?a=1;a=2", queryString.queryString());
      assertEquals(1, queryString.get("a").get(0).intValue());
      assertEquals(2, queryString.get("a").get(1).intValue());
      assertEquals(2, queryString.get("a").size());
      assertEquals(1, queryString.size());
    });
    queryString("/path?a=", queryString -> {
      assertEquals("?a=", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?a=&", queryString -> {
      assertEquals("?a=&", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?a=&&", queryString -> {
      assertEquals("?a=&&", queryString.queryString());
      assertEquals("", queryString.get("a").value());
      assertEquals(1, queryString.size());
    });
    queryString("/path?", queryString -> {
      assertEquals("?", queryString.queryString());
      assertEquals(0, queryString.size());
    });
    queryString("/path", queryString -> {
      assertEquals("", queryString.queryString());
      assertEquals(0, queryString.size());
    });
  }

  @Test
  public void dotNotation() {
    queryString("?user.name=root&user.pwd=pass", queryString -> {
      assertEquals("?user.name=root&user.pwd=pass", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(2, queryString.get("user").size());
      assertEquals("root", queryString.get("user").get("name").value());
      assertEquals("pass", queryString.get("user").get("pwd").value());
    });

    queryString("?0.name=root&0.pwd=pass", queryString -> {
      assertEquals("?0.name=root&0.pwd=pass", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(2, queryString.get(0).size());
      assertEquals("root", queryString.get(0).get("name").value());
      assertEquals("pass", queryString.get(0).get("pwd").value());
    });

    queryString("?user.name=edgar&user.address.street=Street&user.address.number=55&user.type=dev",
        queryString -> {
          assertEquals(
              "?user.name=edgar&user.address.street=Street&user.address.number=55&user.type=dev",
              queryString.queryString());
          assertEquals(1, queryString.size());
          assertEquals(3, queryString.get("user").size());
          assertEquals("edgar", queryString.get("user").get("name").value());
          assertEquals("dev", queryString.get("user").get("type").value());
          assertEquals(2, queryString.get("user").get("address").size());
          assertEquals("Street", queryString.get("user").get("address").get("street").value());
          assertEquals("55", queryString.get("user").get("address").get("number").value());
        });
  }

  @Test
  public void bracketNotation() {
    queryString("?a[b]=1&a[c]=2", queryString -> {
      assertEquals("?a[b]=1&a[c]=2", queryString.queryString());
      assertEquals(1, queryString.size());
      assertEquals(1, queryString.get("a").get("b").intValue());
      assertEquals(2, queryString.get("a").get("c").intValue());
    });

    queryString(
        "?username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
        queryString -> {
          assertEquals(
              "?username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
              queryString.queryString());
          assertEquals(2, queryString.size());
          assertEquals("xyz", queryString.get("username").value());
          assertEquals("AR", queryString.get("address").get("country").get("name").value());
          assertEquals("BA", queryString.get("address").get("country").get("city").value());
          assertEquals("Line1", queryString.get("address").get("line1").value());
          assertEquals("{username=xyz, address={country={name=AR, city=BA}, line1=Line1}}",
              queryString.toString());
        });

//    queryString("?list=1,2,3", queryString -> {
//      assertEquals("?list=1,2,3", queryString.queryString());
//      assertEquals(1, queryString.size());
//      assertEquals(1, queryString.get("list").get(0).intValue());
//      assertEquals(2, queryString.get("list").get(1).intValue());
//      assertEquals(3, queryString.get("list").get(2).intValue());
//      assertEquals("{list=[1, 2, 3]}", queryString.toString());
//    });
  }

  @Test
  public void valueToMap() {
    queryString("?foo=bar", queryString -> {
      assertEquals("{foo=[bar]}", queryString.toMap().toString());
    });
    queryString("?a=1;a=2", queryString -> {
      assertEquals("{a=[1, 2]}", queryString.toMap().toString());
    });
    queryString(
        "?username=xyz&address[country][name]=AR&address[line1]=Line1&address[country][city]=BA",
        queryString -> {
          assertEquals(
              "{username=[xyz], address.country.name=[AR], address.country.city=[BA], address.line1=[Line1]}",
              queryString.toMap().toString());
          assertEquals(
              "{address.country.name=[AR], address.country.city=[BA], address.line1=[Line1]}",
              queryString.get("address").toMap().toString());
          assertEquals("{country.name=[AR], country.city=[BA]}",
              queryString.get("address").get("country").toMap().toString());
          assertEquals("{city=[BA]}",
              queryString.get("address").get("country").get("city").toMap().toString());
        });
  }

  @Test
  public void verifyIllegalAccess() {
    /** Object: */
    queryString("?foo=bar", queryString -> {
      assertThrows(Err.TypeMismatch.class, () -> queryString.value(), "");
      assertThrows(Err.TypeMismatch.class, () -> queryString.value(""));
      assertThrows(Err.Missing.class, () -> queryString.get("a").get("a").get("a").value());
      assertThrows(Err.Missing.class, () -> queryString.get("missing").value());
      assertThrows(Err.Missing.class, () -> queryString.get(0).value());
      assertEquals("missing", queryString.get("missing").value("missing"));
      assertEquals("a", queryString.get("a").get("a").get("a").value("a"));
    });

    /** Array: */
    queryString("?a=1;a=2", queryString -> {
      assertThrows(Err.TypeMismatch.class, () -> queryString.get("a").value());
      assertEquals("1", queryString.get("a").get(0).value());
      assertEquals("2", queryString.get("a").get(1).value());
      assertThrows(Err.Missing.class, () -> queryString.get("a").get("b").value());
      assertThrows(Err.Missing.class, () -> queryString.get("a").get(3).value());
      assertEquals("missing", queryString.get("a").get(3).value("missing"));
    });

    /** Single Property: */
    queryString("?foo=bar", queryString -> {
      assertThrows(Err.Missing.class, () -> queryString.get("foo").get("missing").value());
      assertEquals("bar", queryString.get("foo").get(0).value());
    });

    /** Missing Property: */
    queryString("?", queryString -> {
      assertThrows(Err.Missing.class, () -> queryString.get("foo").get("missing").value());
      assertThrows(Err.Missing.class, () -> queryString.get("foo").get(0).value());
    });
  }

  @Test
  public void decode() {
    queryString("/?name=Pedro%20Picapiedra", queryString -> {
      assertEquals("Pedro Picapiedra", queryString.get("name").value());
    });

    queryString("/?file=js%2Findex.js", queryString -> {
      assertEquals("js/index.js", queryString.get("file").value());
    });

    queryString("/?25=%20%25", queryString -> {
      assertEquals(" %", queryString.get("25").value());
    });

    queryString("/?plus=a+b", queryString -> {
      assertEquals("a b", queryString.get("plus").value());
    });
    queryString("/?tail=a%20%2B", queryString -> {
      assertEquals("a +", queryString.get("tail").value());
    });

  }

  private void queryString(String queryString, Consumer<QueryString> consumer) {
    consumer.accept(UrlParser.queryString(queryString));
  }
}
