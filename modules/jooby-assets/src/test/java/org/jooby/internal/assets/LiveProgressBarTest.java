package org.jooby.internal.assets;

import me.tongfei.progressbar.ProgressBar;
import static org.easymock.EasyMock.expect;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class LiveProgressBarTest {

  @Test
  public void progressBarNotReady() throws Exception {
    int total = 3;
    new MockUnit(ProgressBar.class)
        .run(unit -> {
          LiveProgressBar pb = new LiveProgressBar(t -> unit.get(ProgressBar.class));
          pb.accept(1, total);
        });
  }

  @Test
  public void progressBarReady() throws Exception {
    int total = 4;
    new MockUnit(ProgressBar.class)
        .expect(unit -> {
          ProgressBar pb = unit.get(ProgressBar.class);
          expect(pb.start()).andReturn(pb);
          expect(pb.stepTo(2)).andReturn(pb);

          expect(pb.step()).andReturn(pb);
          expect(pb.step()).andReturn(pb);
          expect(pb.stop()).andReturn(pb);
        })
        .run(unit -> {
          LiveProgressBar pb = new LiveProgressBar(t -> unit.get(ProgressBar.class));
          pb.accept(1, total);
          pb.accept(2, total);
          pb.start();
          pb.accept(3, total);
          pb.accept(4, total);
        });
  }
}
