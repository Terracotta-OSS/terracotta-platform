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
package org.terracotta.dynamic_config.cli.api.output;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class FileOutputService extends StreamOutputService {

  private final Path path;

  public FileOutputService(Path path) throws FileNotFoundException, UnsupportedEncodingException {
    super(new PrintStream(path.toFile(), Charset.defaultCharset().name()), System.err);
    this.path = path;
  }

  @Override
  public void close() {
    out.close();
    // do not close err stream
  }

  @Override
  public String toString() {
    return "file:" + path;
  }
}
