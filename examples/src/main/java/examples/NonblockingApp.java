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
package examples;

import io.jooby.Jooby;
import io.jooby.ExecutionMode;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.SubmissionPublisher;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class NonblockingApp extends Jooby {
  {
    get("/future", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return supplyAsync(() -> ctx.query("n").intValue(1))
          .thenApply(x -> {
            System.out.println("Apply1: " + Thread.currentThread().getName());
            return x * 2;
          })
          .thenApply(x -> {
            System.out.println("Apply2: " + Thread.currentThread().getName());
            return x * 2;
          });
    });

    get("/flowable", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return Flowable.fromCallable(() -> ctx.query("n").intValue(1))
          .map(x -> {
            System.out.println("Apply1: " + Thread.currentThread().getName());
            return x * 2;
          })
          .map(x -> {
            System.out.println("Apply2: " + Thread.currentThread().getName());
            return x * 2;
          })
          .observeOn(Schedulers.computation())
          .subscribeOn(Schedulers.io());
    });

    get("/publisher", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
      publisher.submit("hey");
      publisher.close();
      return publisher;
    });

    get("/single", ctx -> {
      System.out.println("Scheduled: " + Thread.currentThread().getName());
      return Single.fromCallable(() -> ctx.query("n").intValue(1))
          .map(x -> {
            System.out.println("Apply1: " + Thread.currentThread().getName());
            return x * 2;
          })
          .map(x -> {
            System.out.println("Apply2: " + Thread.currentThread().getName());
            return x * 2;
          })
          .observeOn(Schedulers.computation())
          .subscribeOn(Schedulers.io());
    });
  }

  public static void main(String[] args) {
    new NonblockingApp()
        .mode(ExecutionMode.EVENT_LOOP)
        .start()
        .join();
  }
}
