package org.jooby;

import java.util.Set;

import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

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
        .expect("[Basic, Collection, Optional, Enum, Upload, byte[], Date, LocalDate, Locale, bean, valueOf(String), fromString(String), forName(String), init(String)]");
  }
}
