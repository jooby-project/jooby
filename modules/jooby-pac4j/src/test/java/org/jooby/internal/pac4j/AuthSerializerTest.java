package org.jooby.internal.pac4j2;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.jooby.internal.pac4j.AuthSerializer;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthSerializer.class, ByteArrayOutputStream.class, ObjectOutputStream.class,
    ObjectInputStream.class })
public class AuthSerializerTest {

  @Test
  public void emptyConstructor() throws Exception {
    new AuthSerializer();
  }

  @Test
  public void objToStr() throws Exception {
    assertEquals("v", AuthSerializer.objToStr("v"));
    assertEquals("7", AuthSerializer.objToStr(7));
    assertEquals(
        "b64~rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAADdwQAAAADc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAZzcQB+AAIAAAAHc3EAfgACAAAACHg=",
        AuthSerializer.objToStr(Lists.newArrayList(6, 7, 8)));

    try {
      // error
      Object value = new Object();
      new MockUnit()
          .expect(unit -> {
            ByteArrayOutputStream bytes = unit.constructor(ByteArrayOutputStream.class)
                .build();
            ObjectOutputStream stream = unit.constructor(ObjectOutputStream.class)
                .args(OutputStream.class)
                .build(bytes);
            stream.writeObject(value);
            stream.flush();
            expectLastCall().andThrow(new IOException());
          })
          .run(unit -> {
            try {
              AuthSerializer.objToStr(value);
              fail("Serializer must fail");
            } catch (IllegalArgumentException ex) {
              // OK
            }
          });
    } catch (IllegalArgumentException ex) {

    }
  }

  @Test
  public void strToObj() throws Exception {
    assertEquals(null, AuthSerializer.strToObject(null));
    assertEquals("v", AuthSerializer.strToObject("v"));
    assertEquals(Lists.newArrayList(6, 7, 8), AuthSerializer.strToObject(
        "b64~rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAADdwQAAAADc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAZzcQB+AAIAAAAHc3EAfgACAAAACHg="));

    // error
    new MockUnit()
        .expect(unit -> {
          ObjectInputStream stream = unit.constructor(ObjectInputStream.class)
              .args(InputStream.class)
              .build(isA(ByteArrayInputStream.class));
          expect(stream.readObject()).andThrow(new IOException());
        })
        .run(unit -> {
          try {
            AuthSerializer.strToObject("b64~rO0A");
            fail("deserializer must fail");
          } catch (IllegalArgumentException ex) {
            // OK
          }
        });
  }

}
