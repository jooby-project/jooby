package org.jooby.rocker;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Optional;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Renderer.Context;
import org.jooby.Route;
import org.jooby.View;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fizzed.rocker.BindableRockerModel;
import com.fizzed.rocker.RenderingException;
import com.fizzed.rocker.Rocker;
import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerOutputFactory;
import com.fizzed.rocker.RockerTemplate;
import com.fizzed.rocker.RockerTemplateCustomizer;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RockerRenderer.class, Rocker.class, Channels.class, Rocker.class })
public class RockerRenderTest {

  private Block send = unit -> {
    ReadableByteChannel channel = unit.mock(ReadableByteChannel.class);
    ArrayOfByteArraysOutput output = unit.get(ArrayOfByteArraysOutput.class);
    expect(output.getByteLength()).andReturn(1024);
    expect(output.asReadableByteChannel()).andReturn(channel);

    InputStream stream = unit.mock(InputStream.class);

    unit.mockStatic(Channels.class);
    expect(Channels.newInputStream(channel)).andReturn(stream);

    Context context = unit.get(Renderer.Context.class);
    expect(context.type(MediaType.html)).andReturn(context);
    expect(context.length(1024)).andReturn(context);
    context.send(stream);
  };

  @Test
  public void renderNone() throws Exception {
    new RockerRenderer("", ".rocker.html")
        .render(null, null);
  }

  @Test
  public void renderRockerModel() throws Exception {
    new MockUnit(RockerModel.class, Renderer.Context.class)
        .expect(model(RockerModel.class))
        .expect(send)
        .run(unit -> {
          new RockerRenderer("", ".rocker.html")
              .render(unit.get(RockerModel.class), unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderRequestTemplate() throws Exception {
    Map<String, Object> locals = ImmutableMap.of("foo", "bar");
    new MockUnit(RockerModel.class, Renderer.Context.class)
        .expect(model(RockerModel.class))
        .expect(send)
        .expect(unit -> {
          Context context = unit.get(Renderer.Context.class);
          expect(context.locals()).andReturn(locals);
        })
        .run(unit -> {
          new RockerRenderer("", ".rocker.html")
              .render(unit.get(RockerModel.class), unit.get(Renderer.Context.class));
        }, unit -> {
          RequestRockerTemplate template = new RequestRockerTemplate(unit.get(RockerModel.class)) {
            @Override
            protected void __doRender() throws IOException, RenderingException {
            }
          };
          unit.captured(RockerTemplateCustomizer.class).iterator().next().customize(template);
          assertEquals(locals, template.locals);
        });
  }

  @Test
  public void renderTemplate() throws Exception {
    new MockUnit(RockerModel.class, Renderer.Context.class, RockerTemplate.class)
        .expect(model(RockerModel.class))
        .expect(send)
        .run(unit -> {
          new RockerRenderer("", ".rocker.html")
              .render(unit.get(RockerModel.class), unit.get(Renderer.Context.class));
        }, unit -> {
          unit.captured(RockerTemplateCustomizer.class).iterator().next()
              .customize(unit.get(RockerTemplate.class));
        });
  }

  @Test
  public void renderView() throws Exception {
    new MockUnit(BindableRockerModel.class, Renderer.Context.class, View.class)
        .expect(view("", "index"))
        .expect(model(BindableRockerModel.class))
        .expect(send)
        .run(unit -> {
          new RockerRenderer("", ".rocker.html")
              .render(unit.get(View.class), unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderViewWithPrefix() throws Exception {
    new MockUnit(BindableRockerModel.class, Renderer.Context.class, View.class)
        .expect(view("/", "index"))
        .expect(model(BindableRockerModel.class))
        .expect(send)
        .run(unit -> {
          new RockerRenderer("/", ".rocker.html")
              .render(unit.get(View.class), unit.get(Renderer.Context.class));
        });
  }

  @Test
  public void renderViewWithPrefix2() throws Exception {
    new MockUnit(BindableRockerModel.class, Renderer.Context.class, View.class)
        .expect(view("views", "index"))
        .expect(model(BindableRockerModel.class))
        .expect(send)
        .run(unit -> {
          new RockerRenderer("views", ".rocker.html")
              .render(unit.get(View.class), unit.get(Renderer.Context.class));
        });
  }

  @SuppressWarnings("unchecked")
  private Block model(final Class<? extends RockerModel> class1) {
    return unit -> {
      RockerModel model = unit.get(class1);
      ArrayOfByteArraysOutput output = unit.registerMock(ArrayOfByteArraysOutput.class);
      expect(model.render(isA(RockerOutputFactory.class),
          unit.capture(RockerTemplateCustomizer.class))).andReturn(output);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block view(final String prefix, final String name) {
    return unit -> {
      View view = unit.get(View.class);
      expect(view.name()).andReturn(name);
      Map data = ImmutableMap.of();
      expect(view.model()).andReturn(data);
      BindableRockerModel model = unit.get(BindableRockerModel.class);
      expect(model.bind(data)).andReturn(model);
      unit.mockStatic(Rocker.class);
      expect(Rocker.template(Route.normalize(
          Optional.ofNullable(prefix).map(p -> p + "/" + name).orElse(name) + ".rocker.html")
          .substring(1)))
              .andReturn(model);
    };
  }
}
