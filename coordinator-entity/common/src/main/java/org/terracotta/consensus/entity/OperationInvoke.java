/**
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
package org.terracotta.consensus.entity;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Alex Snaps
 */
public class OperationInvoke {

  private final Operation op;
  private final Object[] args;

  public OperationInvoke(final byte[] bytes) throws IOException {
    DataInput input = new DataInputStream(new ByteArrayInputStream(bytes));
    op = readOp(input);
    args = readArgs(input, op);
  }

  public Operation operation() {
    return op;
  }

  public Object[] payload() {
    return args;
  }

  static Operation readOp(final DataInput input) throws IOException {
    byte type = input.readByte();
    return Operation.values()[type];
  }

  static Object[] readArgs(final DataInput input, final Operation op) throws IOException {
    final Class[] argTypes = op.getArgTypes();
    Object[] args = new Object[argTypes.length];
    for (int i = 0, argTypesLength = argTypes.length; i < argTypesLength; i++) {
      final Class aClass = argTypes[i];
      if (aClass == String.class) {
        args[i] = input.readUTF();
      } else if (aClass == long.class) {
        args[i] = input.readLong();
      } else {
        throw new IllegalArgumentException();
      }
    }
    return args;
  }



  enum Operation {

    ENLIST(String.class, String.class),
    ACCEPT(long.class),
    REFUSE(long.class),
    DELIST(String.class, String.class),
    ;

    final Class[] args;

    Operation(final Class... args) {
      this.args = args;
    }

    public Class[] getArgTypes() {
      return args;
    }
  }


}
