package org.jooby.internal.elasticsearch;

import static org.easymock.EasyMock.expect;

import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EmbeddedHandler.class, InternalNode.class })
public class EmbeddedHandlerTest {

  @Test
  public void handle() throws Exception {
    new MockUnit(NodeBuilder.class, Request.class, Response.class)
        .expect(unit -> {
          EmbeddedHttpRequest req = unit.mockConstructor(EmbeddedHttpRequest.class,
              new Class[]{String.class, Request.class }, "/search", unit.get(Request.class));

          EmbeddedHttpChannel channel = unit.mockConstructor(EmbeddedHttpChannel.class,
              new Class[]{RestRequest.class, Response.class, boolean.class }, req,
              unit.get(Response.class), true);
          channel.done();

          RestController controller = unit.mock(RestController.class);
          controller.dispatchRequest(req, channel);

          Injector injector = unit.mock(Injector.class);
          expect(injector.getInstance(RestController.class)).andReturn(controller);

          InternalNode node = unit.mock(InternalNode.class);
          expect(node.injector()).andReturn(injector);

          NodeBuilder nb = unit.get(NodeBuilder.class);
          expect(nb.build()).andReturn(node);

        })
        .run(unit -> {
          new EmbeddedHandler("/search", new ManagedNode(unit.get(NodeBuilder.class)), true)
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

}
