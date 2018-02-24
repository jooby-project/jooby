package org.jooby.internal.mvc;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.mvc.Header;
import org.jooby.mvc.Local;
import org.jooby.test.MockUnit;
import org.junit.Test;

import javax.inject.Named;
import java.lang.reflect.Parameter;
import java.util.Optional;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RequestParamTest {

  public void javax(@Named("javax") final String s) {

  }

  public void ejavax(@Named final String s) {
  }

  public void guice(@com.google.inject.name.Named("guice") final String s) {
  }

  public void header(@Header("H-1") final String s) {
  }

  public void namedheader(@Named("x") @Header final String s) {
  }

  public void eheader(@Header final String s) {
  }
  
  public void local(@Local String myLocal) {
    
  }
  
  @Test
  public void name() throws Exception {
    assertEquals("javax", RequestParam.nameFor(param("javax")));

    assertTrue(RequestParam.nameFor(param("ejavax")) == null
        || "s".equals(RequestParam.nameFor(param("ejavax"))));

    assertEquals("guice", RequestParam.nameFor(param("guice")));

    assertEquals("H-1", RequestParam.nameFor(param("header")));

    assertEquals("x", RequestParam.nameFor(param("namedheader")));

    assertTrue(RequestParam.nameFor(param("eheader")) == null
        || "s".equals(RequestParam.nameFor(param("eheader"))));

  }
  
  @Test
  public void requestParam_mvcLocal_valuePresent() throws Throwable {
    Parameter param = param("local");
    RequestParam requestParam = new RequestParam(param, "myLocal", param.getParameterizedType());
    
    // verify that with a mock request we can indeed retrieve the 'myLocal' value
    new MockUnit(Request.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);
          expect(request.ifGet("myLocal")).andReturn(Optional.of("myCustomValue"));
          verify();
        })
        .run((unit) -> {
          Object output = requestParam.value(unit.get(Request.class), null, null);
          assertEquals("myCustomValue", output);
        });
  }
  
  @Test
  public void requestParam_mvcLocal_valueAbsent() throws Throwable {
    Parameter param = param("local");
    RequestParam requestParam = new RequestParam(param, "myLocal", param.getParameterizedType());
    
    // verify that we return a descriptive error when myLocal could not be located
    new MockUnit(Request.class)
        .expect(unit -> {
          Request request = unit.get(Request.class);
          expect(request.path()).andReturn("/mypath");
          expect(request.ifGet("myLocal")).andReturn(Optional.empty());
          verify();
        })
        .run((unit) -> {
          RuntimeException exception = null;
          try {
            requestParam.value(unit.get(Request.class), null, null);
          } catch(RuntimeException e) {
            exception = e;
          }
          assertNotNull("Should have thrown an exception because the myLocal is not present", exception);
          assertEquals("Server Error(500): Could not find required local 'myLocal', which was required on /mypath", exception.getMessage());
        });
  }
  
  private Parameter param(final String name) throws Exception {
    return RequestParamTest.class.getDeclaredMethod(name, String.class).getParameters()[0];
  }
}
