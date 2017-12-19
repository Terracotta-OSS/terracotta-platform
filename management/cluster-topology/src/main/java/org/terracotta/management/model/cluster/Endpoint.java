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
package org.terracotta.management.model.cluster;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public final class Endpoint implements Serializable {

  private static final long serialVersionUID = 1;

  private final String address;
  private final int port;

  private Endpoint(String address, int port) {
    this.address = Objects.requireNonNull(address);
    this.port = port;
  }

  public String getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    String address = getAddress();
    if (address.contains(":")) {
      address = "[" + address + "]";
    }
    return address + ":" + getPort();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Endpoint endpoint = (Endpoint) o;
    return port == endpoint.port && address.equals(endpoint.address);
  }

  @Override
  public int hashCode() {
    int result = address.hashCode();
    result = 31 * result + port;
    return result;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> endpoint = new LinkedHashMap<String, Object>();
    endpoint.put("address", address);
    endpoint.put("port", port);
    return endpoint;
  }

  public static Endpoint create(String address, int port) {
    return new Endpoint(address, port);
  }

  public static Endpoint valueOf(String str) {
    int lastColon = str.lastIndexOf(':');
    String host = str.substring(0, lastColon);
    String port = str.substring(lastColon+1);

    if (host.startsWith("[")) {
      if (!host.contains("]")) {
        throw new IllegalArgumentException(String.format("Endpoint contains an invalid host '%s'. "
          + "IPv6 address literals must be enclosed in '[' and ']' according to RFC 2732", str));
      }
      host = host.substring(1, host.lastIndexOf(']'));
    }
    return create(host, Integer.parseInt(port));
  }

}
