package org.jooby.js;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Collections;
import java.util.Optional;

import org.jooby.Response;
import org.jooby.Result;
import org.jooby.internal.js.JsResponse;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JsResponse.class, ScriptObjectMirror.class, ImmutableMap.class })
public class JsResponseTest {

  @Test
  public void sendjsObject() throws Exception {
    ImmutableMap<String, Object> value = ImmutableMap.of();
    new MockUnit(Response.class, ScriptObjectMirror.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.status()).andReturn(Optional.empty());
          expect(rsp.type()).andReturn(Optional.empty());

          rsp.send(isA(Result.class));
        })
        .expect(unit -> {
          ScriptObjectMirror mirror = unit.get(ScriptObjectMirror.class);
          expect(mirror.isArray()).andReturn(false);

          unit.mockStatic(ImmutableMap.class);
          expect(ImmutableMap.copyOf(mirror)).andReturn(value);
        })
        .run(unit -> {
          new JsResponse(unit.get(Response.class))
              .sendjs(unit.get(ScriptObjectMirror.class));
          ;
        });
  }

  @Test
  public void sendjsArray() throws Exception {
    new MockUnit(Response.class, ScriptObjectMirror.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);

          expect(rsp.status()).andReturn(Optional.empty());
          expect(rsp.type()).andReturn(Optional.empty());

          rsp.send(isA(Result.class));
        })
        .expect(unit -> {
          ScriptObjectMirror mirror = unit.get(ScriptObjectMirror.class);
          expect(mirror.isArray()).andReturn(true);
          expect(mirror.entrySet()).andReturn(Collections.emptySet());
        })
        .run(unit -> {
          new JsResponse(unit.get(Response.class))
              .sendjs(unit.get(ScriptObjectMirror.class));
          ;
        });
  }

}
