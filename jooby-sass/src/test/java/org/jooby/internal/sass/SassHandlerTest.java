package org.jooby.internal.sass;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.internal.sass.SassHandler;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SassHandler.class, URL.class })
public class SassHandlerTest {

  @Test
  public void resolve() throws Exception {
    assertNotNull(new SassHandler().resolve("/sass/sass.scss"));
  }

  @Test
  public void sendFromFile() throws Exception {
    new MockUnit(Request.class, Response.class, Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);

          expect(asset.resource()).andReturn(
              new File("src/test/resources/sass/sass.scss").toURI().toURL());
          expect(asset.type()).andReturn(MediaType.css);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.type(MediaType.css)).andReturn(rsp);
          rsp.send(unit.capture(Result.class));
        })
        .run(unit -> {
          new SassHandler().send(unit.get(Request.class), unit.get(Response.class),
              unit.get(Asset.class));
        }, unit -> {
          Result result = unit.captured(Result.class).iterator().next();
          assertEquals("body {\n" +
              "\tfont: 100% Helvetica, sans-serif;\n" +
              "\tcolor: #333;\n" +
              "}", result.get().get());
        });
  }

  @Test
  public void sendFromResource() throws Exception {
    new MockUnit(Request.class, Response.class, Asset.class)
        .expect(unit -> {
          URL resource = unit.mock(URL.class);
          expect(resource.toExternalForm()).andReturn("!/sass/sass.scss");

          Asset asset = unit.get(Asset.class);

          expect(asset.resource()).andReturn(resource);
          expect(asset.type()).andReturn(MediaType.css);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.type(MediaType.css)).andReturn(rsp);
          rsp.send(unit.capture(Result.class));
        })
        .run(unit -> {
          new SassHandler().send(unit.get(Request.class), unit.get(Response.class),
              unit.get(Asset.class));
        }, unit -> {
          Result result = unit.captured(Result.class).iterator().next();
          assertEquals("body {\n" +
              "\tfont: 100% Helvetica, sans-serif;\n" +
              "\tcolor: #333;\n" +
              "}", result.get().get());
        });
  }

}
