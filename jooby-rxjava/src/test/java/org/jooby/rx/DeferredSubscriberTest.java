package org.jooby.rx;

import org.jooby.Deferred;
import org.jooby.rx.Rx.DeferredSubscriber;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class DeferredSubscriberTest {

  @Test
  public void newObject() throws Exception {
    new MockUnit(Deferred.class)
        .run(unit -> {
          new Rx.DeferredSubscriber(unit.get(Deferred.class));
        });
  }

  @Test
  public void onComplete() throws Exception {
    new MockUnit(Deferred.class)
        .expect(unit -> {
          Deferred deferred = unit.get(Deferred.class);
          deferred.resolve((Object) null);
        })
        .run(unit -> {
          DeferredSubscriber subscriber = new Rx.DeferredSubscriber(unit.get(Deferred.class));
          subscriber.onCompleted();
          // ignored
          subscriber.onCompleted();
        });
  }

  @Test
  public void onError() throws Exception {
    Throwable cause = new Throwable();
    new MockUnit(Deferred.class)
        .expect(unit -> {
          Deferred deferred = unit.get(Deferred.class);
          deferred.reject(cause);
        })
        .run(unit -> {
          new Rx.DeferredSubscriber(unit.get(Deferred.class))
              .onError(cause);
        });
  }

  @Test
  public void onNext() throws Exception {
    Object value = new Object();
    new MockUnit(Deferred.class)
        .expect(unit -> {
          Deferred deferred = unit.get(Deferred.class);
          deferred.resolve(value);
        })
        .run(unit -> {
          DeferredSubscriber subscriber = new Rx.DeferredSubscriber(unit.get(Deferred.class));
          subscriber.onNext(value);
          // ignored
          subscriber.onNext(value);
          // ignored
          subscriber.onCompleted();
        });
  }

}
