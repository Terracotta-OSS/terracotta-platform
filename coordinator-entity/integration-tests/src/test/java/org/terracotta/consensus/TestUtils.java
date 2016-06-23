/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.terracotta.consensus.entity.CoordinationServerEntityService;
import org.terracotta.consensus.entity.client.ClientCoordinationEntityService;
import org.terracotta.passthrough.PassthroughServer;

/**
 *
 * @author cdennis
 */
public final class TestUtils {
  
  private TestUtils() {
    //singleton
  }
  
  public static PassthroughServer createServer() {
    PassthroughServer server = new PassthroughServer();
    server.registerServerEntityService(new CoordinationServerEntityService());
    server.registerClientEntityService(new ClientCoordinationEntityService());
    server.start(true, false);
    return server;
  }
  
  public static <T> Future<T> inOtherThread(Callable<T> task) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      return executor.submit(task);
    } finally {
      executor.shutdown();
    }
  }
}
