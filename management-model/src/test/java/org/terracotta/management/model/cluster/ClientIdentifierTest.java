package org.terracotta.management.model.cluster;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ClientIdentifierTest {

  @Test
  public void test_identifier() throws UnknownHostException {
    ClientIdentifier identifier = ClientIdentifier.create("my-app");
    System.out.println(identifier);

    System.out.println(ClientIdentifier.create());

    assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), identifier.getPid());
    assertNotEquals("127.0.0.1", identifier.getHostAddress());

    assertEquals(ClientIdentifier.create("my-app"), identifier);
    assertEquals(ClientIdentifier.create(ClientIdentifier.discoverPID(), ClientIdentifier.discoverLANAddress().getHostAddress(), "my-app", ""), identifier);
    assertEquals(ClientIdentifier.valueOf(ClientIdentifier.discoverPID() + "@" + ClientIdentifier.discoverLANAddress().getHostAddress() + ":my-app"), identifier);

    identifier = ClientIdentifier.valueOf("123@127.0.0.1:jetty:app:3");
    assertEquals("123@127.0.0.1:jetty:app:3", identifier.getClientId());
    assertEquals("123@127.0.0.1:jetty:app", identifier.getProductId());
    assertEquals("123@127.0.0.1", identifier.getVmId());
  }

}
