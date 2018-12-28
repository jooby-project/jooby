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
package io.jooby;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public interface Server {

  abstract class Base implements Server {

    private AtomicBoolean stopping = new AtomicBoolean();

    protected void fireStart(List<Jooby> applications, Supplier<Executor> workerProvider) {
      Executor serverWorker = null;
      for (Jooby app : applications) {
        Executor worker = app.worker();
        if (worker == null) {
          if (serverWorker == null) {
            // server worker is shared between app
            serverWorker = workerProvider.get();
          }
          app.worker(serverWorker);
        }
        app.start(this);
      }
    }

    protected void fireReady(List<Jooby> applications) {
      for (Jooby app : applications) {
        app.ready(this);
      }
    }

    protected void fireStop(List<Jooby> applications) {
      if (stopping.compareAndSet(false, true)) {
        if (applications != null) {
          for (Jooby app : applications) {
            app.stop();
          }
        }
      }
    }

    protected void addShutdownHook() {
      Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }
  }

  /** 16KB constant. */
  int _16KB = 0x4000;

  int port();

  @Nonnull Server port(int port);

  @Nonnull Server deploy(Jooby application);

  @Nonnull Server start();

  @Nonnull default void join() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  @Nonnull Server stop();

  @Nonnull Server gzip(boolean enabled);

}
