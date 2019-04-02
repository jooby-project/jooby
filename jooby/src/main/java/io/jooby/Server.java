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
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Server {

  abstract class Base implements Server {

    private AtomicBoolean stopping = new AtomicBoolean();

    protected void fireStart(List<Jooby> applications, Executor defaultWorker) {
      for (Jooby app : applications) {
        app.setDefaultWorker(defaultWorker).start(this);
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

  int _4KB = 4096;

  int _8KB = 8192;

  /** 16KB constant. */
  int _16KB = 16384;

  int _10MB = 20971520;

  @Nonnull Server setOptions(@Nonnull ServerOptions options);

  @Nonnull ServerOptions getOptions();

  @Nonnull Server start(Jooby application);

  @Nonnull default void join() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  @Nonnull Server stop();

  static boolean connectionLost(Throwable cause) {
    if (cause instanceof IOException) {
      String message = cause.getMessage();
      if (message != null) {
        String msg = message.toLowerCase();
        return msg.contains("reset by peer") || msg.contains("broken pipe");
      }
    }
    return (cause instanceof ClosedChannelException);
  }
}
