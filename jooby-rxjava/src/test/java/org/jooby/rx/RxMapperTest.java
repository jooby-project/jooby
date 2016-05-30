package org.jooby.rx;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import org.jooby.Deferred;
import org.jooby.rx.Rx.DeferredSubscriber;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.Subscription;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Rx.class, Observable.class, Single.class, Completable.class,
    DeferredSubscriber.class })
@SuppressWarnings({"rawtypes", "unchecked" })
public class RxMapperTest {

  private Block obsSubscribeInit = unit -> {
    Observable value = unit.powerMock(Observable.class);
    unit.registerMock(Observable.class, value);
  };

  private Block sSubscribeInit = unit -> {
    Single single = unit.powerMock(Single.class);
    expect(single.toObservable()).andReturn(unit.get(Observable.class));

    unit.registerMock(Single.class, single);
  };

  private Block cSubscribeInit = unit -> {
    Completable single = unit.powerMock(Completable.class);

    expect(single.toObservable()).andReturn(unit.get(Observable.class));
    unit.registerMock(Completable.class, single);
  };

  private Block deferredSubscriber = unit -> {
    DeferredSubscriber subscriber = unit.constructor(DeferredSubscriber.class)
        .args(Deferred.class)
        .build(isA(Deferred.class));
    unit.registerMock(DeferredSubscriber.class, subscriber);
  };

  private Block obsSubscribe = unit -> {
    Observable value = unit.get(Observable.class);

    DeferredSubscriber subscriber = unit.get(DeferredSubscriber.class);

    expect(value.subscribe(subscriber)).andReturn(null);
  };

  private Block scheduler = unit -> {
    Scheduler scheduler = unit.mock(Scheduler.class);
    unit.registerMock(Scheduler.class, scheduler);
  };

  @Test
  public void rxObservable() throws Exception {
    new MockUnit(Observable.class, Subscription.class)
        .expect(obsSubscribeInit)
        .expect(deferredSubscriber)
        .expect(obsSubscribe)
        .run(unit -> {
          Deferred deferred = (Deferred) Rx.rx().map(unit.get(Observable.class));
          deferred.handler((r, x) -> {
          });
        });
  }

  @Test
  public void rxObservableWithScheduler() throws Exception {
    new MockUnit(Observable.class, Subscription.class)
        .expect(obsSubscribeInit)
        .expect(deferredSubscriber)
        .expect(scheduler)
        .expect(obsSubscribe)
        .expect(unit -> {
          Observable single = unit.get(Observable.class);

          expect(single.observeOn(unit.get(Scheduler.class))).andReturn(single);
        })
        .run(unit -> {
          Deferred deferred = (Deferred) Rx.rx(o -> o.observeOn(unit.get(Scheduler.class)))
              .map(unit.get(Observable.class));
          deferred.handler((r, x) -> {
          });
        });
  }

  @Test
  public void rxSingle() throws Exception {
    new MockUnit()
        .expect(obsSubscribeInit)
        .expect(sSubscribeInit)
        .expect(deferredSubscriber)
        .expect(obsSubscribe)
        .run(unit -> {
          Deferred deferred = (Deferred) Rx.rx().map(unit.get(Single.class));
          deferred.handler((r, x) -> {
          });
        });
  }

  @Test
  public void rxComplete() throws Exception {
    new MockUnit()
        .expect(obsSubscribeInit)
        .expect(cSubscribeInit)
        .expect(deferredSubscriber)
        .expect(obsSubscribe)
        .run(unit -> {
          Deferred deferred = (Deferred) Rx.rx().map(unit.get(Completable.class));
          deferred.handler((r, x) -> {
          });
        });
  }

  @Test
  public void rxNone() throws Exception {
    new MockUnit()
        .run(unit -> {
          Object value = new Object();
          assertEquals(value, Rx.rx().map(value));
        });
  }

}
