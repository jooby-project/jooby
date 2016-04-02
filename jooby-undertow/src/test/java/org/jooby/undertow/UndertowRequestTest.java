package org.jooby.undertow;

import static org.easymock.EasyMock.expect;

import org.jooby.internal.undertow.UndertowRequest;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowRequest.class, HttpServerExchange.class, HeaderMap.class })
public class UndertowRequestTest {

  private Block form = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.getString("application.tmpdir")).andReturn("target");
    expect(conf.getString("application.charset")).andReturn("UTF-8");

    HeaderMap headers = unit.mock(HeaderMap.class);
    expect(headers.getFirst("Content-Type")).andReturn(null);

    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.getRequestHeaders()).andReturn(headers);
    expect(exchange.getRequestPath()).andReturn("/");
  };

  @Test
  public void newObject() throws Exception {
    new MockUnit(HttpServerExchange.class, Config.class)
        .expect(form)
        .run(unit -> {
          new UndertowRequest(unit.get(HttpServerExchange.class), unit.get(Config.class));
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unknownUpgrade() throws Exception {
    new MockUnit(HttpServerExchange.class, Config.class)
        .expect(form)
        .run(unit -> {
          new UndertowRequest(unit.get(HttpServerExchange.class), unit.get(Config.class))
              .upgrade(Object.class);
        });
  }

}
