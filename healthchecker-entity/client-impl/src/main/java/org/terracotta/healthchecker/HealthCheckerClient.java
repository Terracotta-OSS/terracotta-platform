/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.healthchecker;

import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

/**
 *
 */
public class HealthCheckerClient implements HealthCheck {
  
  private final EntityClientEndpoint<HealthCheckReq, HealthCheckRsp> endpoint;
  private final Timer driver;

  public HealthCheckerClient(EntityClientEndpoint<HealthCheckReq, HealthCheckRsp> endpoint) {
    this.endpoint = endpoint;
    this.driver = new Timer("healthcheck timer - " + endpoint.hashCode(), true);
    this.endpoint.setDelegate(new HealthCheckerDelegate());
  }

  @Override
  public Future<String> ping(String message) {
    try {
//  don't add any extra acks here.  This is pure ping-pong
      return wrapFuture(endpoint.beginInvoke().message(new HealthCheckReq(message)).replicate(false).invoke());
    } catch (MessageCodecException codec) {
      throw new RuntimeException(codec);
    }
  }
  
  private Future<String> wrapFuture(final InvokeFuture<HealthCheckRsp> invoke) {
    return new Future<String>() {
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return  false;
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return invoke.isDone();
      }

      @Override
      public String get() throws InterruptedException, ExecutionException {
        try {
//  not intended for use but implemented in case a future use arises
          return invoke.get().toString();
        } catch (EntityException ee) {
          throw new ExecutionException(ee);
        }
      }

      @Override
      public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
          return invoke.getWithTimeout(timeout, unit).toString();
        } catch (EntityException ee) {
          throw new ExecutionException(ee);
        }
      } 
    };
  }

  @Override
  public void close() {
    endpoint.close();
    driver.cancel();
  }

  @Override
  public Timer getTimer() {
    return driver;
  }

  class HealthCheckerDelegate implements EndpointDelegate {
    @Override
    public void handleMessage(EntityResponse messageFromServer) {
  // do nothing
    }

    @Override
    public byte[] createExtendedReconnectData() {
  // do nothing
      return new byte[0];
    }

    @Override
    public void didDisconnectUnexpectedly() {
      driver.cancel();
    }
  }
}
