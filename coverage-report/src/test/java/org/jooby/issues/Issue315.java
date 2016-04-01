package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue315 extends ServerFeature {

  {
    AtomicInteger inc = new AtomicInteger(1);

    after("/err", (req, rsp, cause) -> {
      assertTrue(cause.isPresent());
    });

    get("/err", () -> {
      throw new IllegalStateException();
    });

    err((req, rsp, err) -> {
      rsp.send("err");
    });

    get("direct-inc", () -> inc.get());

    before("/rsp", (req, rsp) -> {
      rsp.send("before");
    });

    StringBuilder buff = new StringBuilder();

    get("/direct-buff", () -> buff);

    before("/buff", (req, rsp) -> {
      buff.append("a");
    });

    after("/buff", (req, rsp, cause) -> {
      buff.append("b");
    });
    get("/buff", () -> buff);

    before("/chain", (req, rsp, result) -> {
      String v = result.get();
      result.set("<" + v + ">");
      return result;
    });

    before("/chain", (req, rsp, result) -> {
      String v = result.get();
      result.set("-" + v + "-");
      return result;
    });

    get("/chain", () -> "v");

    after("/async", (req, rsp, cause) -> {
      assertEquals(false, cause.isPresent());
    });

    ExecutorService executor = Executors.newSingleThreadExecutor();
    get("/async", promise(deferred -> {
      executor.execute(deferred.run(() -> "async"));
    }));

    before((req, rsp) -> {
      inc.incrementAndGet();
    });

    before((req, rsp, result) -> {
      Integer i = result.get();
      result.set(i + 2);
      return result;
    });

    after((req, rsp, cause) -> {
      inc.incrementAndGet();
    });

    get("/inc", () -> inc.get());
  }

  @Test
  public void shouldInvokeBeforeAfterHandlers() throws Exception {
    request()
        .get("/inc")
        .expect("4");

    request()
        .get("/direct-inc")
        .expect("3");
  }

  @Test
  public void shouldGenerateAResponseFromBeforeSend() throws Exception {
    request()
        .get("/rsp")
        .expect("before");
  }

  @Test
  public void beforeChain() throws Exception {
    request()
        .get("/chain")
        .expect("<-v->");
  }

  @Test
  public void sendWithAfter() throws Exception {
    request()
        .get("/buff")
        .expect("a");

    request()
        .get("/direct-buff")
        .expect("ab");
  }

  @Test
  public void async() throws Exception {
    request()
        .get("/async")
        .expect("async");
  }

  @Test
  public void afterWithErr() throws Exception {
    request()
        .get("/err")
        .expect("err");
  }

}
