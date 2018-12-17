/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Flow;

public class ContextSubscriber implements Subscriber<Object>, Flow.Subscriber<Object> {

  private final Context ctx;

  public ContextSubscriber(Context ctx) {
    this.ctx = ctx;
  }

  @Override public void onSubscribe(Subscription s) {
    s.request(Long.MAX_VALUE);
  }

  @Override public void onSubscribe(Flow.Subscription s) {
    s.request(Long.MAX_VALUE);
  }

  @Override public void onNext(Object value) {
    ctx.render(value);
  }

  @Override public void onError(Throwable x) {
    ctx.sendError(x);
  }

  @Override public void onComplete() {
  }
}
