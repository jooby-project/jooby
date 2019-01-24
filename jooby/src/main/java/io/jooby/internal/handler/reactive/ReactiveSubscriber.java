/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ReactiveSubscriber implements Subscriber<Object> {

  private final ChunkedSubscriber subscriber;

  public ReactiveSubscriber(ChunkedSubscriber subscriber) {
    this.subscriber = subscriber;
  }

  @Override public void onSubscribe(Subscription s) {
    subscriber.onSubscribe(new ReactiveSubscription(s));
  }

  @Override public void onNext(Object item) {
    subscriber.onNext(item);
  }

  @Override public void onError(Throwable x) {
    subscriber.onError(x);
  }

  @Override public void onComplete() {
    subscriber.onComplete();
  }
}
