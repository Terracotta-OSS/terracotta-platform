/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.platform;

import com.tc.classloader.CommonComponent;

import static java.util.Objects.requireNonNull;

@CommonComponent
public class ServerInfo {
  private final String name;

  // Constructor must be public because this class is marked @CommonComponent so gets loaded in a different classloader
  // which means that, whilst the package name is the same as the package for ServerInfoProvider, the logical package
  // is different, so package visibility on the constructor is not adequate.
  public ServerInfo(String name) {
    this.name = requireNonNull(name, "name");
  }

  public String getName() {
    return name;
  }
}
