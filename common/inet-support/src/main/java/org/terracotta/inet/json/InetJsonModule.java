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
package org.terracotta.inet.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.terracotta.inet.InetSocketAddressConverter;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public class InetJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public InetJsonModule() {
    super(InetJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    addSerializer(InetSocketAddress.class, ToStringSerializer.instance);
    addDeserializer(InetSocketAddress.class, new FromStringDeserializer<InetSocketAddress>(InetSocketAddress.class) {
      private static final long serialVersionUID = 1L;

      @Override
      protected InetSocketAddress _deserialize(String value, DeserializationContext ctxt) {
        return InetSocketAddressConverter.getInetSocketAddress(value);
      }
    });
  }
}
