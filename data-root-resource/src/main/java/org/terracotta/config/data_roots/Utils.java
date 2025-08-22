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
package org.terracotta.config.data_roots;

import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
  public static boolean overLaps(Path existing, Path newPath) throws IOException {
    Path realExistingPath = existing.toRealPath();
    Path realNewPath = newPath.toRealPath();
    return realExistingPath.startsWith(realNewPath) || realNewPath.startsWith(realExistingPath);
  }

  public static boolean isEmpty(Path path) throws InvalidConfigChangeException {
    if (Files.isDirectory(path)) {
      try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
        return !directory.iterator().hasNext();
      } catch (IOException e) {
        throw new InvalidConfigChangeException(e.toString(), e);
      }
    }
    return false;
  }
}
