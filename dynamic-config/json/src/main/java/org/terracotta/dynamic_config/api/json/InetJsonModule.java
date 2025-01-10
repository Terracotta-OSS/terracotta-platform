/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.json;

import org.terracotta.inet.HostPort;
import org.terracotta.inet.InetSocketAddressConverter;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public class InetJsonModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.objectToString(InetSocketAddress.class, InetSocketAddressConverter::parseInetSocketAddress);
    config.objectToString(HostPort.class, HostPort::parse);
  }
}
