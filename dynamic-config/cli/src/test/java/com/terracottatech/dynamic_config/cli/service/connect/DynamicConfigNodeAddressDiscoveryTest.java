/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.connect;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.DiagnosticServiceProvider;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Tuple2;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.connection.ConnectionException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigNodeAddressDiscoveryTest {

  @Mock public DiagnosticService diagnosticService;
  @Mock public TopologyService topologyService;
  @Mock public Cluster cluster;

  @Test
  public void test_discover() {
    DynamicConfigNodeAddressDiscovery discovery = new DynamicConfigNodeAddressDiscovery(new DiagnosticServiceProvider("foo", 1, TimeUnit.SECONDS, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, long connectTimeout, TimeUnit connectTimeUnit) {
        return diagnosticService;
      }
    }, 60, TimeUnit.SECONDS);

    when(diagnosticService.getProxy(TopologyService.class)).thenReturn(topologyService);
    when(topologyService.getTopology()).thenReturn(cluster);
    when(topologyService.getThisNodeAddress()).thenReturn(InetSocketAddress.createUnresolved("localhost", 9410));
    when(cluster.getNodeAddresses()).thenReturn(Arrays.asList(
        InetSocketAddress.createUnresolved("localhost", 9410),
        InetSocketAddress.createUnresolved("1.2.3.4", 9411),
        InetSocketAddress.createUnresolved("1.2.3.5", 9410),
        InetSocketAddress.createUnresolved("1.2.3.5", 9411)
    ));

    InetSocketAddress anAddress = InetSocketAddress.createUnresolved("127.0.0.1", 9410);
    Tuple2<InetSocketAddress, Collection<InetSocketAddress>> nodes = discovery.discover(anAddress);

    assertThat(nodes.t1, is(equalTo(InetSocketAddress.createUnresolved("localhost", 9410))));
    assertThat(nodes.t2, Matchers.hasSize(4));
    assertThat(nodes.t2, Matchers.containsInAnyOrder(
        InetSocketAddress.createUnresolved("localhost", 9410),
        InetSocketAddress.createUnresolved("1.2.3.4", 9411),
        InetSocketAddress.createUnresolved("1.2.3.5", 9410),
        InetSocketAddress.createUnresolved("1.2.3.5", 9411)
    ));
  }

  @Test
  public void test_failures() {
    Stream.of(
        new IllegalStateException("a random exception"),
        new ConnectionException(new Throwable()),
        new EntityNotFoundException("cname", "ename"),
        new EntityNotProvidedException("cname", "ename"),
        new EntityVersionMismatchException("cname", "ename", 1, 2)
    ).forEach(ex -> assertThat(
        () -> new DynamicConfigNodeAddressDiscovery(fetchThrowing(ex), 60, TimeUnit.SECONDS).discover(InetSocketAddress.createUnresolved("1.2.3.4", 9410)),
        is(throwing(instanceOf(NodeAddressDiscoveryException.class)).andCause(is(ex)))
    ));
  }

  private DiagnosticServiceProvider fetchThrowing(Throwable throwable) {
    return new DiagnosticServiceProvider("foo", 1, TimeUnit.SECONDS, null) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, long connectTimeout, TimeUnit connectTimeUnit) {
        forceThrow(throwable);
        fail();
        return diagnosticService;
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void forceThrow(Throwable throwable) throws E {
    // this is a trick to allow testing with checked exception ConnectionException, EntityNotFoundException, EntityNotProvidedException, EntityVersionMismatchException
    // plus also runtime exception without having to overload the method
    // See here for more information:
    // https://stackoverflow.com/questions/11942946/how-to-throw-an-exception-when-your-method-signature-doesnt-allow-to-throw-exce
    throw (E) throwable;
  }
}