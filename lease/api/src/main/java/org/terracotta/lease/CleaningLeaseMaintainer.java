/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.lease;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

class CleaningLeaseMaintainer implements LeaseMaintainer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CleaningLeaseMaintainer.class);

  private final LeaseMaintainer delegate;
  private final Connection connection;
  private final List<Closeable> resources;


  CleaningLeaseMaintainer(LeaseMaintainer delegate, Connection connection, Closeable... resources) {
    this(delegate, connection, Arrays.asList(resources));
  }

  private CleaningLeaseMaintainer(LeaseMaintainer delegate, Connection connection, List<Closeable> resources) {
    this.delegate = delegate;
    this.connection = connection;
    this.resources = resources;
  }

  @Override
  public Lease getCurrentLease() {
    return delegate.getCurrentLease();
  }

  @Override
  public void waitForLease() throws InterruptedException {
    delegate.waitForLease();
  }

  @Override
  public boolean waitForLease(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return delegate.waitForLease(timeout, timeUnit);
  }

  @Override
  public void close() throws IOException {
    closeResources();
    delegate.close();
  }

  @Override
  public void destroy() throws IOException {
    closeResources();
    connection.close();
  }

  private void closeResources() {
    for (Closeable resource : resources) {
      try {
        resource.close();
      } catch (Throwable t) {
        LOGGER.info("Exception closing resource: " + resource, t);
      }
    }
  }
}
