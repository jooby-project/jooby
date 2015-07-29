package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jooby.MediaType;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.ByteStreams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({URLAsset.class, URL.class })
public class URLAssetTest {

  @Test
  public void name() throws Exception {
    assertEquals("pom.xml",
        new URLAsset(file("pom.xml").toURI().toURL(), "pom.xml", MediaType.js)
            .name());
  }

  @Test
  public void toStr() throws Exception {
    assertEquals("URLAssetTest.js(application/javascript)",
        new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
            "URLAssetTest.js", MediaType.js)
            .toString());
  }

  @Test
  public void path() throws Exception {
    assertEquals("/path/URLAssetTest.js", new URLAsset(getClass().getResource("URLAssetTest.js"),
        "/path/URLAssetTest.js", MediaType.js).path());
  }

  @Test
  public void lastModified() throws Exception {
    assertTrue(new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI()
        .toURL(), "URLAssetTest.js", MediaType.js)
        .lastModified() > 0);
  }

  @Test
  public void lastModifiedFileNotFound() throws Exception {
    assertTrue(new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.missing")
        .toURI().toURL(), "URLAssetTest.missing", MediaType.js)
        .lastModified() == -1);
  }

  @Test(expected = Exception.class)
  public void headerFailNoConnection() throws Exception {
    new MockUnit(URL.class)
        .expect(unit -> {
          URL url = unit.get(URL.class);
          expect(url.openConnection()).andThrow(new Exception("intentional err"));
        })
        .run(unit -> {
          new URLAsset(unit.get(URL.class), "path.js", MediaType.js);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void headerFailWithConnection() throws Exception {
    new MockUnit(URL.class)
        .expect(unit -> {
          InputStream stream = unit.mock(InputStream.class);
          stream.close();

          URLConnection conn = unit.mock(URLConnection.class);
          conn.setUseCaches(false);
          expect(conn.getContentLengthLong()).andThrow(
              new IllegalStateException("intentional err"));
          expect(conn.getInputStream()).andReturn(stream);

          URL url = unit.get(URL.class);
          expect(url.getProtocol()).andReturn("http");
          expect(url.openConnection()).andReturn(conn);
        })
        .run(unit -> {
          new URLAsset(unit.get(URL.class), "pa.ks", MediaType.js);
        });
  }

  @Test
  public void noLastModifiednoLen() throws Exception {
    new MockUnit(URL.class)
        .expect(unit -> {
          InputStream stream = unit.mock(InputStream.class);
          stream.close();

          URLConnection conn = unit.mock(URLConnection.class);
          conn.setUseCaches(false);
          expect(conn.getContentLengthLong()).andReturn(0L);
          expect(conn.getLastModified()).andReturn(0L);
          expect(conn.getInputStream()).andReturn(stream);

          URL url = unit.get(URL.class);
          expect(url.getProtocol()).andReturn("http");
          expect(url.openConnection()).andReturn(conn);
        })
        .run(unit -> {
          URLAsset asset = new URLAsset(unit.get(URL.class), "pa.ks", MediaType.js);
          assertEquals(-1, asset.length());
          assertEquals(-1, asset.lastModified());
        });
  }

  @Test(expected = IllegalStateException.class)
  public void headersStreamCloseFails() throws Exception {
    new MockUnit(URL.class)
        .expect(unit -> {
          InputStream stream = unit.mock(InputStream.class);
          stream.close();
          expectLastCall().andThrow(new IOException("ignored"));

          URLConnection conn = unit.mock(URLConnection.class);
          conn.setUseCaches(false);
          expect(conn.getContentLengthLong()).andThrow(
              new IllegalStateException("intentional err"));
          expect(conn.getInputStream()).andReturn(stream);

          URL url = unit.get(URL.class);
          expect(url.getProtocol()).andReturn("http");
          expect(url.openConnection()).andReturn(conn);
        })
        .run(unit -> {
          new URLAsset(unit.get(URL.class), "ala.la", MediaType.js);
        });
  }

  @Test
  public void length() throws Exception {
    assertEquals(15, new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js")
        .toURI().toURL(), "URLAssetTest.js",
        MediaType.js).length());
  }

  @Test
  public void type() throws Exception {
    assertEquals(MediaType.js,
        new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
            "URLAssetTest.js", MediaType.js)
            .type());
  }

  @Test
  public void stream() throws Exception {
    InputStream stream = new URLAsset(
        file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
        "URLAssetTest.js", MediaType.js)
        .stream();
    assertEquals("function () {}\n", new String(ByteStreams.toByteArray(stream)));
    stream.close();
  }

  @Test(expected = NullPointerException.class)
  public void nullFile() throws Exception {
    new URLAsset((URL) null, "", MediaType.js);
  }

  @Test(expected = NullPointerException.class)
  public void nullType() throws Exception {
    new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
        "", null);
  }

  /**
   * Attempt to load a file from multiple location. required by unit and integration tests.
   *
   * @param location
   * @return
   */
  private File file(final String location) {
    for (String candidate : new String[]{location, "jooby/" + location, "../jooby/" + location }) {
      File file = new File(candidate);
      if (file.exists()) {
        return file;
      }
    }
    return new File(location);
  }
}
