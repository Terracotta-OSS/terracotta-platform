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
package org.terracotta.statecollector;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;

class StateCollectorImpl implements StateCollector {
  
  private final EntityClientEndpoint<StateCollectorMessage, StateCollectorMessage> entityClientEndpoint;
  private volatile Runnable platformStateCollector;
  
  public StateCollectorImpl(final EntityClientEndpoint<StateCollectorMessage, StateCollectorMessage> entityClientEndpoint) {
    this.entityClientEndpoint = entityClientEndpoint;
    entityClientEndpoint.setDelegate(new EndpointDelegate() {
      @Override
      public void handleMessage(final EntityResponse entityResponse) {
        if(entityResponse instanceof StateCollectorMessage) {
          StateCollectorMessageType type = ((StateCollectorMessage) entityResponse).getType();
          switch (type) {
            case DUMP:
              ThreadDumpUtil.getThreadDump();
              if(platformStateCollector != null) {
                platformStateCollector.run();
              }
              break;
            default:
              throw new IllegalArgumentException("Unknown message type: " + type);
          }
        }
        throw new IllegalArgumentException("Unknown message: " + entityResponse);
      }

      @Override
      public byte[] createExtendedReconnectData() {
        return new byte[0];
      }

      @Override
      public void didDisconnectUnexpectedly() {
      }
    });
  }

  @Override
  public void setPlatformStateCollector(final Runnable platformStateCollector) {
    this.platformStateCollector = platformStateCollector;
  }
  
  @Override
  public void close() {
    entityClientEndpoint.close();
  }
}
