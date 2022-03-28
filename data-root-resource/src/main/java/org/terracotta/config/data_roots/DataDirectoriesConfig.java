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
package org.terracotta.config.data_roots;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.PlatformConfiguration;

import java.io.Closeable;

import static java.util.Optional.ofNullable;

/**
 * API for looking up configured data directories using unique names.
 * <p>
 * Usually user configures one or more data directories with absolute path in the server's configuration along
 * with a unique name and this service is used for retrieving these mappings.
 *
 * <p>An example configuration</p>
 *
 * <pre>{@code
 * <tc-config>
 *  <plugins>
 *    <config>
 *      <data:data-directories>
 *        <data:directory name="a">/some/path/a</data:directory>
 *        <data:directory name="b">/some/path/b</data:directory>
 *      </data:data-directories>
 *    </config>
 *  </plugins>
 * </tc-config>
 * }</pre>
 *
 *
 * <p>Note that this service doesn't provide any name-spacing guarantees. As this service could be used by multiple entities,
 * entity implementors should use some kind of isolation to store data to avoid polluting data directories</p>
 */
@CommonComponent
public interface DataDirectoriesConfig extends Closeable {

  static String cleanStringForPath(String input) {
    return ofNullable(input).orElse("").replace(":", "-");
  }

  /**
   * Returns a {@link DataDirectories} that will append the server name to all paths.
   * <p>
   * Since the server name cannot be injected during configuration parsing
   * as we do not yet know which server is being started, we have to resort to this trick and make sure it gets used.
   *
   * @param platformConfiguration the platform configuration to source the server name from
   * @return a {@code DataDirectories}
   */
  DataDirectories getDataDirectoriesForServer(PlatformConfiguration platformConfiguration);

  void addDataDirectory(String name, String path);

  void validateDataDirectory(String name, String path);
}