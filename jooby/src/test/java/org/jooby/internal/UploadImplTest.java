package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.internal.reqparam.ParserExecutor;
import org.jooby.spi.NativeUpload;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Injector;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UploadImpl.class, MutantImpl.class })
public class UploadImplTest {

  @Test
  public void close() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class)
        .expect(unit -> {
          unit.get(NativeUpload.class).close();
        })
        .run(unit -> {
          new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).close();
        });
  }

  @Test
  public void name() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class)
        .expect(unit -> {
          expect(unit.get(NativeUpload.class).name()).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x",
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).name());
        });
  }

  @Test
  public void describe() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class)
        .expect(unit -> {
          expect(unit.get(NativeUpload.class).name()).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x",
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).toString());
        });
  }

  @Test
  public void file() throws Exception {
    File f = new File("x");
    new MockUnit(Injector.class, NativeUpload.class)
        .expect(unit -> {
          expect(unit.get(NativeUpload.class).file()).andReturn(f);
        })
        .run(unit -> {
          assertEquals(f,
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).file());
        });
  }

  @Test
  public void type() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class, ParserExecutor.class)
        .expect(unit -> {
          expect(unit.get(Injector.class).getInstance(ParserExecutor.class)).andReturn(
              unit.get(ParserExecutor.class));
        })
        .expect(
            unit -> {
              NativeUpload upload = unit.get(NativeUpload.class);

              List<String> headers = Arrays.asList("application/json");
              expect(upload.headers("Content-Type")).andReturn(headers);

              StrParamReferenceImpl pref = unit.mockConstructor(StrParamReferenceImpl.class, new Class[]{
                  String.class, List.class }, "Content-Type", headers);

              Mutant mutant = unit.mockConstructor(MutantImpl.class,
                  new Class[]{ParserExecutor.class, Object.class },
                  unit.get(ParserExecutor.class), pref);

              expect(mutant.toOptional(MediaType.class))
                  .andReturn(Optional.ofNullable(MediaType.json));
            })
        .run(unit -> {
          assertEquals(MediaType.json,
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).type());
        });
  }

  @Test
  public void deftype() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class, ParserExecutor.class)
        .expect(unit -> {
          expect(unit.get(Injector.class).getInstance(ParserExecutor.class)).andReturn(
              unit.get(ParserExecutor.class));
        })
        .expect(unit -> {
          expect(unit.get(NativeUpload.class).name()).andReturn("x");
        })
        .expect(unit -> {
          NativeUpload upload = unit.get(NativeUpload.class);

          List<String> headers = Arrays.asList();
          expect(upload.headers("Content-Type")).andReturn(headers);

          StrParamReferenceImpl pref = unit.mockConstructor(StrParamReferenceImpl.class, new Class[]{
              String.class, List.class }, "Content-Type", headers);

          Mutant mutant = unit.mockConstructor(MutantImpl.class,
              new Class[]{ParserExecutor.class, Object.class },
              unit.get(ParserExecutor.class), pref);

          expect(mutant.toOptional(MediaType.class))
              .andReturn(Optional.ofNullable(null));
        })
        .run(unit -> {
          assertEquals(MediaType.octetstream,
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).type());
        });
  }

  @Test
  public void typeFromName() throws Exception {
    new MockUnit(Injector.class, NativeUpload.class, ParserExecutor.class)
        .expect(unit -> {
          expect(unit.get(Injector.class).getInstance(ParserExecutor.class)).andReturn(
              unit.get(ParserExecutor.class));
        })
        .expect(unit -> {
          expect(unit.get(NativeUpload.class).name()).andReturn("x.js");
        })
        .expect(unit -> {
          NativeUpload upload = unit.get(NativeUpload.class);

          List<String> headers = Arrays.asList();
          expect(upload.headers("Content-Type")).andReturn(headers);

          StrParamReferenceImpl pref = unit.mockConstructor(StrParamReferenceImpl.class, new Class[]{
              String.class, List.class }, "Content-Type", headers);

          Mutant mutant = unit.mockConstructor(MutantImpl.class,
              new Class[]{ParserExecutor.class, Object.class },
              unit.get(ParserExecutor.class), pref);

          expect(mutant.toOptional(MediaType.class))
              .andReturn(Optional.ofNullable(null));
        })
        .run(unit -> {
          assertEquals(MediaType.js,
              new UploadImpl(unit.get(Injector.class), unit.get(NativeUpload.class)).type());
        });
  }

}
