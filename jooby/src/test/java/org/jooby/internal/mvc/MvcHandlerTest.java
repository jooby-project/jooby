package org.jooby.internal.mvc;

import static org.easymock.EasyMock.expect;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MvcHandler.class })
public class MvcHandlerTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Method.class, Object.class, RequestParamProvider.class)
        .run(unit -> {
          new MvcHandler(unit.get(Method.class), unit.get(Object.class).getClass(), unit.get(RequestParamProvider.class));
        });
  }

  @Test
  public void handleNOOP() throws Exception {
    new MockUnit(Method.class, Object.class, RequestParamProvider.class, Request.class, Response.class)
        .run(unit -> {
          new MvcHandler(unit.get(Method.class), unit.get(Object.class).getClass(), unit.get(RequestParamProvider.class))
          .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void handle() throws Exception {
    Class handlerClass = MvcHandlerTest.class;
    MvcHandlerTest handler = new MvcHandlerTest();
    Method method = handlerClass.getDeclaredMethod("strhandle");
    new MockUnit(RequestParamProvider.class, Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.require(MvcHandlerTest.class)).andReturn(handler);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(false);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          rsp.send("strhandle");
          unit.get(Route.Chain.class).next(unit.get(Request.class), rsp);
        })
        .expect(unit -> {
          List<RequestParam> params = Collections.emptyList();
          RequestParamProvider paramProvider = unit.get(RequestParamProvider.class);
          expect(paramProvider.parameters(method)).andReturn(params);
        })
        .run(unit -> {
          new MvcHandler(method, handlerClass, unit.get(RequestParamProvider.class))
              .handle(unit.get(Request.class), unit.get(Response.class), unit.get(Route.Chain.class));
        });
  }
  
  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void handleAbstractHandlers() throws Exception {
	Class handlerClass = FinalMvcHandler.class;
	Class abstractHandlerClass = AbstractMvcHandler.class;
    FinalMvcHandler handler = new FinalMvcHandler();
    Method method = abstractHandlerClass.getDeclaredMethod("abstrStrHandle");
    new MockUnit(RequestParamProvider.class, Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.require(FinalMvcHandler.class)).andReturn(handler);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(false);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          rsp.send("abstrStrHandle");
          unit.get(Route.Chain.class).next(unit.get(Request.class), rsp);
        })
        .expect(unit -> {
          List<RequestParam> params = Collections.emptyList();
          RequestParamProvider paramProvider = unit.get(RequestParamProvider.class);
          expect(paramProvider.parameters(method)).andReturn(params);
        })
        .run(unit -> {
          new MvcHandler(method, handlerClass, unit.get(RequestParamProvider.class))
              .handle(unit.get(Request.class), unit.get(Response.class), unit.get(Route.Chain.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test(expected = IOException.class)
  public void handleException() throws Exception {
    Class handlerClass = MvcHandlerTest.class;
    MvcHandlerTest handler = new MvcHandlerTest();
    Method method = handlerClass.getDeclaredMethod("errhandle");
    new MockUnit(RequestParamProvider.class, Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.require(MvcHandlerTest.class)).andReturn(handler);
        })
        .expect(unit -> {
          List<RequestParam> params = Collections.emptyList();
          RequestParamProvider paramProvider = unit.get(RequestParamProvider.class);
          expect(paramProvider.parameters(method)).andReturn(params);
        })
        .run(unit -> {
          new MvcHandler(method, handlerClass, unit.get(RequestParamProvider.class))
              .handle(unit.get(Request.class), unit.get(Response.class), unit.get(Route.Chain.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test(expected = Throwable.class)
  public void throwableException() throws Exception {
    Class handlerClass = MvcHandlerTest.class;
    MvcHandlerTest handler = new MvcHandlerTest();
    Method method = handlerClass.getDeclaredMethod("throwablehandle");
    new MockUnit(RequestParamProvider.class, Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.require(MvcHandlerTest.class)).andReturn(handler);
        })
        .expect(unit -> {
          List<RequestParam> params = Collections.emptyList();
          RequestParamProvider paramProvider = unit.get(RequestParamProvider.class);
          expect(paramProvider.parameters(method)).andReturn(params);
        })
        .run(unit -> {
          new MvcHandler(method, handlerClass, unit.get(RequestParamProvider.class))
              .handle(unit.get(Request.class), unit.get(Response.class), unit.get(Route.Chain.class));
        });
  }

  public String strhandle() throws Exception {
    return "strhandle";
  }

  public String errhandle() throws Exception {
    throw new IOException("intentional err");
  }

  public String throwablehandle() throws Throwable {
    throw new Throwable("intentional err");
  }
}

abstract class AbstractMvcHandler {
	public abstract String abstrStrHandle() throws Exception;
	
}

final class FinalMvcHandler extends AbstractMvcHandler {
	public String abstrStrHandle() throws Exception {
		return "abstrStrHandle";
	}
}
