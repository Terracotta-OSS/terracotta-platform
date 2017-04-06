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
package org.terracotta.voltron.proxy.server;

/**
 * This interface can be extened by a proxy interface, and the proxy interface be implemented by entities.
 * <p>
 * This allows an entity to use the {@link org.terracotta.entity.IEntityMessenger} capabilities through a
 * defined proxy interface
 * <p>
 * Calls gets transformed and sent to active / passive according to annotations put on the proxy interface.
 * <p>
 * Works like a standard entity proxy interface
 *
 * @author Mathieu Carbou
 */
public interface Messenger {
  void unSchedule();
}
