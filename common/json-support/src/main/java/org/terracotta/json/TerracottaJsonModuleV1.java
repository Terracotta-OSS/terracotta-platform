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
package org.terracotta.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Mathieu Carbou
 * @deprecated DO NOT USE THIS MAPPER. It is kept as reference about what was used in Fix 1.
 */
@Deprecated
public class TerracottaJsonModuleV1 extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public TerracottaJsonModuleV1() {
    super(TerracottaJsonModuleV1.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    addSerializer(Path.class, ToStringSerializer.instance);
    addDeserializer(Path.class, new FromStringDeserializer<Path>(Path.class) {
      private static final long serialVersionUID = 1L;

      @Override
      protected Path _deserialize(String value, DeserializationContext ctxt) {
        return Paths.get(value);
      }

      @Override
      protected Path _deserializeFromEmptyString() throws IOException {
        return Paths.get("");
      }
    });
  }
}
