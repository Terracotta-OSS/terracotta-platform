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
package org.terracotta.lease.service.closer;

import org.terracotta.entity.ClientDescriptor;

/**
 * A proxy pattern implementation of ClientConnectionCloser that allows LeaseState (which has a ClientConnectionCloser
 * as a dependency) to be constructed before the ClientCommunicator is available. We would not need this class if
 * services could directly find their dependencies.
 */
public class ProxyClientConnectionCloser implements ClientConnectionCloser {
  private volatile ClientConnectionCloser delegate;

  public void setClientConnectionCloser(ClientConnectionCloser delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("You cannot set the delegate ClientConnectionCloser to null");
    }
    this.delegate = delegate;
  }

  @Override
  public void closeClientConnection(ClientDescriptor clientDescriptor) {
    ClientConnectionCloser currentDelegate = delegate;

    if (currentDelegate == null) {
      throw new IllegalStateException("No calls to closeClientConnection() should be made before the delegate ClientConnectionCloser has been set");
    }

    currentDelegate.closeClientConnection(clientDescriptor);
  }
}
