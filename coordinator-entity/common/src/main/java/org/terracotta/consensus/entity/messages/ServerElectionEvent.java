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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.consensus.entity.messages;

import java.io.Serializable;

/**
 *
 * @author cdennis
 */
public class ServerElectionEvent<K> implements Serializable {
  
  public static <K> ServerElectionEvent<K> completed(K key) {
    return new ServerElectionEvent<K>(key, Type.COMPLETED);
  }
  
  public static <K> ServerElectionEvent<K> changed(K key) {
    return new ServerElectionEvent<K>(key, Type.CHANGED);
  }

  public static enum Type {
    COMPLETED, CHANGED;
  }
  
  private final K namespace;
  private final Type type;

  private ServerElectionEvent(K key, Type type) {
    this.namespace = key;
    this.type = type;
  }
  
  public Type getType() {
    return type;
  }

  public K getNamespace() {
    return namespace;
  }
}
