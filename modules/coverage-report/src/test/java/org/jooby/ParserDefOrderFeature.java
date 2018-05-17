package org.jooby;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import java.util.Set;

public class ParserDefOrderFeature extends ServerFeature {

  Key<Set<Parser>> KEY = Key.get(new TypeLiteral<Set<Parser>>() {
  });

  {

    get("/parser/order", req -> req.require(KEY));

  }

  @Test
  public void order() throws Exception {
    request()
        .get("/parser/order")
        .expect("[Basic, Collection, Optional, Enum, byte[], Date, LocalDate, ZonedDateTime, Locale, valueOf(String), fromString(String), forName(String), init(String), bean]");
  }
}
