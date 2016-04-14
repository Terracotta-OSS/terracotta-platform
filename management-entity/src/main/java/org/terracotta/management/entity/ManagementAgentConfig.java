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
package org.terracotta.management.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
public final class ManagementAgentConfig {

  // name must be hardcoded because it reference a class name in client package and is used on server-side
  public static final String ENTITY_NAME = "org.terracotta.management.entity.client.ManagementAgentEntity";

  public byte[] serialize() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeUTF("hello world");
      dos.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot serialize config", e);
    }
  }

  public static ManagementAgentConfig deserialize(byte[] data) {
    try {
      DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
      dis.readUTF();
      return new ManagementAgentConfig();
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot deserialize config", e);
    }
  }

}
