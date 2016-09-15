package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.ByteStreams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BodyReferenceImpl.class, ByteStreams.class, FileOutputStream.class, Files.class,
    File.class, ByteArrayOutputStream.class })
public class BodyReferenceImplTest {

  private Block mkdir = unit -> {
    File dir = unit.mock(File.class);
    expect(dir.mkdirs()).andReturn(true);

    File file = unit.get(File.class);
    expect(file.getParentFile()).andReturn(dir);
  };

  private Block fos = unit -> {
    FileOutputStream fos = unit.constructor(FileOutputStream.class)
        .build(unit.get(File.class));

    unit.registerMock(FileOutputStream.class, fos);
  };

  @Test
  public void fromBytes() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(copy(ByteArrayOutputStream.class))
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize);
        });
  }

  @Test(expected = IOException.class)
  public void inErr() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(unit -> {
          InputStream in = unit.get(InputStream.class);

          expect(in.read(unit.capture(byte[].class))).andThrow(new IOException());

          OutputStream out = unit.get(ByteArrayOutputStream.class);

          in.close();
          out.close();
        })
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize);
        });
  }

  @Test(expected = IOException.class)
  public void outErr() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(unit -> {
          InputStream in = unit.get(InputStream.class);
          in.close();

          OutputStream out = unit.get(ByteArrayOutputStream.class);
          out.close();
          expectLastCall().andThrow(new IOException());

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(in, out)).andReturn(1L);
        })
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize);
        });
  }

  @Test(expected = IOException.class)
  public void inErrOnClose() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(unit -> {
          InputStream in = unit.get(InputStream.class);
          in.close();
          expectLastCall().andThrow(new IOException());

          OutputStream out = unit.get(ByteArrayOutputStream.class);
          out.close();

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(in, out)).andReturn(1L);
        })
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize);
        });
  }

  @Test
  public void fromFile() throws Exception {
    long len = 1;
    long bsize = -1;

    new MockUnit(File.class, InputStream.class)
        .expect(mkdir)
        .expect(fos)
        .expect(copy(FileOutputStream.class))
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize);
        });
  }

  @Test
  public void bytesFromBytes() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(copy(ByteArrayOutputStream.class))
        .run(unit -> {
          byte[] rsp = new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize).bytes();
          assertArrayEquals(bytes, rsp);
        });
  }

  @Test
  public void bytesFromFile() throws Exception {
    long len = 1;
    long bsize = -1;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class, Path.class)
        .expect(mkdir)
        .expect(fos)
        .expect(copy(FileOutputStream.class))
        .expect(unit -> {
          expect(unit.get(File.class).toPath()).andReturn(unit.get(Path.class));

          unit.mockStatic(Files.class);
          expect(Files.readAllBytes(unit.get(Path.class))).andReturn(bytes);
        })
        .run(unit -> {
          byte[] rsp = new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize)
                  .bytes();
          assertEquals(bytes, rsp);
        });
  }

  @Test
  public void textFromBytes() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class)
        .expect(baos(bytes))
        .expect(copy(ByteArrayOutputStream.class))
        .run(unit -> {
          String rsp = new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize).text();
          assertEquals("bytes", rsp);
        });
  }

  @Test
  public void textFromFile() throws Exception {
    long len = 1;
    long bsize = -1;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class, Path.class)
        .expect(mkdir)
        .expect(fos)
        .expect(copy(FileOutputStream.class))
        .expect(unit -> {
          expect(unit.get(File.class).toPath()).andReturn(unit.get(Path.class));

          unit.mockStatic(Files.class);
          expect(Files.readAllBytes(unit.get(Path.class))).andReturn(bytes);
        })
        .run(unit -> {
          String rsp = new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize)
                  .text();
          assertEquals("bytes", rsp);
        });
  }

  @Test
  public void writeToFromFile() throws Exception {
    long len = 1;
    long bsize = -1;
    new MockUnit(File.class, InputStream.class, Path.class, OutputStream.class)
        .expect(mkdir)
        .expect(fos)
        .expect(copy(FileOutputStream.class))
        .expect(unit -> {
          expect(unit.get(File.class).toPath()).andReturn(unit.get(Path.class));

          unit.mockStatic(Files.class);
          expect(Files.copy(unit.get(Path.class), unit.get(OutputStream.class))).andReturn(1L);
        })
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize)
                  .writeTo(unit.get(OutputStream.class));
        });
  }

  @Test
  public void bytesWriteTo() throws Exception {
    long len = 1;
    long bsize = 2;
    byte[] bytes = "bytes".getBytes();
    new MockUnit(File.class, InputStream.class, OutputStream.class)
        .expect(baos(bytes))
        .expect(copy(ByteArrayOutputStream.class))
        .expect(unit -> {
          unit.get(OutputStream.class).write(bytes);
        })
        .run(unit -> {
          new BodyReferenceImpl(len, StandardCharsets.UTF_8, unit.get(File.class),
              unit.get(InputStream.class), bsize)
                  .writeTo(unit.get(OutputStream.class));
        });
  }

  private Block copy(final Class<? extends OutputStream> oclass) {
    return copy(oclass, true);
  }

  private Block copy(final Class<? extends OutputStream> oclass, final boolean close) {
    return unit -> {

      InputStream in = unit.get(InputStream.class);

      OutputStream out = unit.get(oclass);

      if (close) {
        in.close();
        out.close();
      }

      unit.mockStatic(ByteStreams.class);
      expect(ByteStreams.copy(in, out)).andReturn(1L);
    };
  }

  private Block baos(final byte[] bytes) {
    return unit -> {
      ByteArrayOutputStream baos = unit.constructor(ByteArrayOutputStream.class)
          .build();

      expect(baos.toByteArray()).andReturn(bytes);

      unit.registerMock(ByteArrayOutputStream.class, baos);
    };
  }

}
