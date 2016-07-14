package org.jooby.internal.couchbase;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.CouchbaseAsyncRepository;

public class SetConverterHackTest {

  @Test
  public void setConverter() {
    new SetConverterHack();
    SetConverterHack.forceConverter(new CouchbaseAsyncRepository(null), new JacksonMapper());
  }

  @Test
  public void setConverterFailure() throws Exception {
    new MockUnit(AsyncRepository.class)
        .run(unit -> {
          SetConverterHack.forceConverter(unit.get(AsyncRepository.class), new JacksonMapper());
        });
  }
}
