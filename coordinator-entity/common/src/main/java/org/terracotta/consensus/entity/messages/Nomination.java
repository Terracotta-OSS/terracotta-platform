/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.consensus.entity.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Alex Snaps
 */
public class Nomination implements Serializable {

  static final int AWAITS_ELECTION = -1;

  private volatile long id;
  private final String namespace;
  private final boolean elected;

  public Nomination(String namespace) {
    this.id = AWAITS_ELECTION;
    this.namespace = namespace;
    this.elected = false;
  }

  public Nomination(final String namespace, final long id, boolean elected) {
    if (id == AWAITS_ELECTION) {
      throw new IllegalArgumentException();
    }
    this.id = id;
    this.namespace = namespace;
    this.elected = elected;
  }

  public boolean awaitsElection() {
    return id == AWAITS_ELECTION;
  }
  
  public String getNamespace() {
    return namespace;
  }
  
  public boolean isContinuing() {
    return !this.elected;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }   
    if (getClass() != obj.getClass()) {
      return false;
    }
    return id == ((Nomination) obj).id;
  }
  
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    out.writeLong(this.id);
  }

  private void readObject(ObjectInputStream in) throws IOException,ClassNotFoundException {
    in.defaultReadObject();
    this.id = in.readLong();
  }
}
