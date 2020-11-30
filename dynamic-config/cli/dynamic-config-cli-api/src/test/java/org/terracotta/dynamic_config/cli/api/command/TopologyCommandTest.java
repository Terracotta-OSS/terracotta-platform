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
package org.terracotta.dynamic_config.cli.api.command;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.api.BaseTest;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.cli.api.command.Injector.inject;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.NODE;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommandTest<C extends TopologyCommand> extends BaseTest {

  @Test
  public void test_defaults() {
    C command = newCommand();
    assertThat(command.getOperationType(), is(equalTo(NODE)));
  }

  protected final C newCommand() {
    return inject(newTopologyCommand(), asList(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, objectMapperFactory, nomadEntityProvider));
  }

  protected abstract C newTopologyCommand();
}