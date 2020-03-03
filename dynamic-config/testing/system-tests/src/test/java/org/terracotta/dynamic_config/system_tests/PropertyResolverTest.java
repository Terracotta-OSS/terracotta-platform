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
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.util.PropertyResolver;

import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class PropertyResolverTest {

  @Test
  public void resolve() {
    Properties variables = new Properties();
    variables.setProperty("foo", "1");
    variables.setProperty("bar", "2");
    variables.setProperty("path", "c:\\a\\win\\path");
    PropertyResolver resolver = new PropertyResolver(variables);
    assertThat(resolver.resolve(null), is(nullValue()));
    assertThat(resolver.resolve("${a}"), is(equalTo("${a}")));
    assertThat(resolver.resolve("${foo}"), is(equalTo("1")));
    assertThat(resolver.resolve("${foo}${bar}"), is(equalTo("12")));
    assertThat(resolver.resolve("path/to/${foo}/and/${bar}"), is(equalTo("path/to/1/and/2")));
    assertThat(resolver.resolve("this is the ${path}"), is(equalTo("this is the c:\\a\\win\\path")));
  }

  @Test
  public void resolveAll() {
    Properties variables = new Properties();
    variables.setProperty("foo", "1");
    variables.setProperty("bar", "2");
    PropertyResolver resolver = new PropertyResolver(variables);

    Properties p = new Properties();
    p.setProperty("a", "");
    p.setProperty("b", "${a}");
    p.setProperty("c", "${foo}");
    p.setProperty("d", "${foo}${bar}");
    p.setProperty("e", "path/to/${foo}/and/${bar}");

    p = resolver.resolveAll(p);

    assertThat(p.entrySet(), hasSize(5));
    assertThat(p.getProperty("a"), is(equalTo("")));
    assertThat(p.getProperty("b"), is(equalTo("${a}")));
    assertThat(p.getProperty("c"), is(equalTo("1")));
    assertThat(p.getProperty("d"), is(equalTo("12")));
    assertThat(p.getProperty("e"), is(equalTo("path/to/1/and/2")));
  }
}