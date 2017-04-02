package org.jooby.rocker;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.fizzed.rocker.RenderingException;
import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerTemplate;
import com.fizzed.rocker.runtime.DefaultRockerTemplate;
import com.google.common.collect.ImmutableMap;

public class RequestRockerTemplateTest {

  @Test
  public void setLocals() throws Exception {
    new MockUnit(RockerTemplate.class, RockerModel.class)
        .run(unit -> {
          RequestRockerTemplate ctx = template(unit.get(RockerModel.class));
          Map<String, Object> locals = ImmutableMap.of();
          ctx.locals = locals;
          RequestRockerTemplate template = template(unit.get(RockerModel.class));
          template.__associate(ctx);
          assertEquals(ctx.locals, template.locals);
        });
  }

  @Test(expected = NullPointerException.class)
  public void shouldCallSuperAssociate() throws Exception {
    new MockUnit(DefaultRockerTemplate.class, RockerModel.class)
        .run(unit -> {
          DefaultRockerTemplate ctx = unit.get(DefaultRockerTemplate.class);
          RequestRockerTemplate template = template(unit.get(RockerModel.class));
          template.__associate(ctx);
        });
  }

  private RequestRockerTemplate template(final RockerModel model) {
    return new RequestRockerTemplate(model) {
      @Override
      protected void __doRender() throws IOException, RenderingException {
      }
    };
  }
}
