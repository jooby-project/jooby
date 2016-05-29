package org.jooby.issues;

import org.jooby.reactor.Reactor;
import org.jooby.rx.Rx;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import reactor.core.publisher.Flux;
import rx.Observable;

public class Issue367 extends ServerFeature {

  {
    map(Rx.rx());

    map(Reactor.reactor());

    get("/rx", () -> Observable.just("reactive"));

    get("/flux", () -> Flux.just("reactive"));

    get("/normal", () -> "no-reactive");
  }

  @Test
  public void globalMapShouldWork() throws Exception {
    request()
        .get("/rx")
        .expect("reactive");

    request()
        .get("/flux")
        .expect("reactive");

    request()
        .get("/normal")
        .expect("no-reactive");
  }

}
