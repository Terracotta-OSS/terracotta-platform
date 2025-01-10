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
package org.terracotta.dynamic_config.cli.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.terracotta.inet.HostPort;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public class ShapeConverter implements IStringConverter<Map.Entry<Collection<HostPort>, String>> {
  @Override
  public Map.Entry<Collection<HostPort>, String> convert(String stripe) {
    // -stripe <[name/]hostname[:port]|hostname[:port]|...>
    // LinkedHashMap: to keep stripe ordering as user defined them
    // Set: node ordering within a stripe is not important
    final String[] split = stripe.split("/");
    switch (split.length) {
      case 1:
        return new AbstractMap.SimpleEntry<>(new LinkedHashSet<>(HostPort.parse(split[0].split("\\|"), 9410)), null);
      case 2:
        return new AbstractMap.SimpleEntry<>(new LinkedHashSet<>(HostPort.parse(split[1].split("\\|"), 9410)), split[0]);
      default:
        throw new ParameterException("Wrong stripe format");
    }
  }
}
