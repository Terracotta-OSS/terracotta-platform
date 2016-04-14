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
