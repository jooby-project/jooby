package org.jooby.internal;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionResetByPeerTest {

  @Test
  public void isConnectionResetByPeer() {
    new ConnectionResetByPeer();
    assertTrue(ConnectionResetByPeer.test(new IOException("connection reset by Peer")));
    assertFalse(ConnectionResetByPeer.test(new IOException()));
    assertFalse(ConnectionResetByPeer.test(new IllegalStateException("connection reset by peer")));
  }
}
