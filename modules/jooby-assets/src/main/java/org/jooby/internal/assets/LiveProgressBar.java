/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.assets;

import me.tongfei.progressbar.ProgressBar;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Progress bar for LiveCompiler.
 */
public class LiveProgressBar implements BiConsumer<Integer, Integer> {

  private final Function<Integer, ProgressBar> factory;
  private volatile int progress;
  private volatile int total;
  private volatile ProgressBar pb;

  public LiveProgressBar(Function<Integer, ProgressBar> factory) {
    this.factory = factory;
  }

  public LiveProgressBar() {
    this(total -> new ProgressBar("Compiling assets", total));
  }

  @Override public void accept(Integer progress, Integer total) {
    this.progress = progress;
    this.total = total;
    if (pb != null) {
      pb.step();
      if (this.progress == this.total) {
        pb.stop();
      }
    }
  }

  public void start() {
    pb = factory.apply(total);
    pb.start().stepTo(progress);
  }
}
