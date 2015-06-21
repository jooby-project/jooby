package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BodyReferenceImpl.class, Files.class })
public class BodyReferenceImplTest {

  @Test
  public void defaults() throws Exception {
    File file = new File("target", Integer.toHexString(System.identityHashCode(this)));
    new MockUnit(InputStream.class)
        .expect(unit -> {
          Path path = file.toPath();

          InputStream stream = unit.get(InputStream.class);
          stream.close();

          unit.mockStatic(Files.class);
          expect(Files.copy(stream, path)).andReturn(1L);
        })
        .run(unit -> {
          new BodyReferenceImpl(1, null, file, unit.get(InputStream.class));
        });
  }

  @Test(expected = IOException.class)
  public void defaultsReadFailure() throws Exception {
    File file = new File("target", Integer.toHexString(System.identityHashCode(this)));
    new MockUnit(InputStream.class)
        .expect(unit -> {
          Path path = file.toPath();

          InputStream stream = unit.get(InputStream.class);
          stream.close();

          unit.mockStatic(Files.class);
          expect(Files.copy(stream, path)).andThrow(new IOException("intentional err"));
        })
        .run(unit -> {
          new BodyReferenceImpl(1, null, file, unit.get(InputStream.class));
        });
  }

  @Test
  public void defaultsNoContent() throws Exception {
    File file = new File("target", Integer.toHexString(System.identityHashCode(this)));
    new MockUnit(InputStream.class)
        .expect(unit -> {
          new BodyReferenceImpl(0, null, file, unit.get(InputStream.class));
        });
  }

  @Test
  public void writeTo() throws Exception {
    File file = new File("target", Integer.toHexString(System.identityHashCode(this)));
    new MockUnit(InputStream.class, OutputStream.class)
        .expect(unit -> {
          Path path = file.toPath();

          InputStream in = unit.get(InputStream.class);
          in.close();

          unit.mockStatic(Files.class);
          expect(Files.copy(in, path)).andReturn(1L);

          expect(Files.copy(path, unit.get(OutputStream.class))).andReturn(1L);
        })
        .run(unit -> {
          new BodyReferenceImpl(1, null, file, unit.get(InputStream.class))
              .writeTo(unit.get(OutputStream.class));
          ;
        });
  }

}
