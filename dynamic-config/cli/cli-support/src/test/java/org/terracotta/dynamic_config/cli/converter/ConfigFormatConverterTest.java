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
package org.terracotta.dynamic_config.cli.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.ConfigFormat;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.ConfigFormat.CONFIG;
import static org.terracotta.dynamic_config.api.model.ConfigFormat.JSON;
import static org.terracotta.dynamic_config.api.model.ConfigFormat.PROPERTIES;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class ConfigFormatConverterTest {
  @Test
  public void convert() {
    IStringConverter<ConfigFormat> converter = new ConfigFormatConverter();
    assertThat(converter.convert("name"), is(equalTo(CONFIG)));
    assertThat(converter.convert("cfg"), is(equalTo(CONFIG)));
    assertThat(converter.convert("conf"), is(equalTo(CONFIG)));
    assertThat(converter.convert("config"), is(equalTo(CONFIG)));
    assertThat(converter.convert("json"), is(equalTo(JSON)));
    assertThat(converter.convert("index"), is(equalTo(PROPERTIES)));
    assertThat(converter.convert("properties"), is(equalTo(PROPERTIES)));
    assertThat(() -> converter.convert(""), is(throwing(instanceOf(ParameterException.class))));
  }
}