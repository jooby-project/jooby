package org.jooby.jdbi;

import com.google.inject.Key;
import com.google.inject.name.Names;
import static org.easymock.EasyMock.expect;
import org.jdbi.v3.core.Handle;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class TransactionalRequestTest {
  @Test
  public void properties() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    TransactionalRequest request = new TransactionalRequest();
    request.pattern("foo");
    request.method("put");
    request.attach(TransactionalRequestTest.class);
    request.handle("trx");
    request.doWith(h -> {
      latch.countDown();
    });

    assertEquals("foo", request.pattern());
    assertEquals("put", request.method());
    assertEquals(Arrays.asList(TransactionalRequestTest.class), request.sqlObjects());
    assertEquals(Key.get(Handle.class, Names.named("trx")), request.handle());
    request.configurer.accept(null);
    latch.await();
  }
}
