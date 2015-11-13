package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.URL;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class AssetForwardingTest {

  @Test
  public void etag() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.etag()).andReturn("tag");
        })
        .run(unit -> {
          assertEquals("tag", new Asset.Forwarding(unit.get(Asset.class)).etag());
        });
  }

  @Test
  public void lastModified() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.lastModified()).andReturn(1L);
        })
        .run(unit -> {
          assertEquals(1L, new Asset.Forwarding(unit.get(Asset.class)).lastModified());
        });
  }

  @Test
  public void len() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.length()).andReturn(1L);
        })
        .run(unit -> {
          assertEquals(1L, new Asset.Forwarding(unit.get(Asset.class)).length());
        });
  }

  @Test
  public void name() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.name()).andReturn("n");
        })
        .run(unit -> {
          assertEquals("n", new Asset.Forwarding(unit.get(Asset.class)).name());
        });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.path()).andReturn("p");
        })
        .run(unit -> {
          assertEquals("p", new Asset.Forwarding(unit.get(Asset.class)).path());
        });
  }

  @Test
  public void url() throws Exception {
    new MockUnit(Asset.class, URL.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.resource()).andReturn(unit.get(URL.class));
        })
        .run(unit -> {
          assertEquals(unit.get(URL.class), new Asset.Forwarding(unit.get(Asset.class)).resource());
        });
  }

  @Test
  public void stream() throws Exception {
    new MockUnit(Asset.class, InputStream.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.stream()).andReturn(unit.get(InputStream.class));
        })
        .run(unit -> {
          assertEquals(unit.get(InputStream.class),
              new Asset.Forwarding(unit.get(Asset.class)).stream());
        });
  }

  @Test
  public void type() throws Exception {
    new MockUnit(Asset.class)
        .expect(unit -> {
          Asset asset = unit.get(Asset.class);
          expect(asset.type()).andReturn(MediaType.css);
        })
        .run(unit -> {
          assertEquals(MediaType.css, new Asset.Forwarding(unit.get(Asset.class)).type());
        });
  }

}
