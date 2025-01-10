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
package org.terracotta.dynamic_config.cli.api.command;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.cli.api.BaseTest;

import java.nio.file.Paths;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.cli.api.command.ActivateActionTest.rethrow;
import static org.terracotta.dynamic_config.cli.api.command.Injector.inject;

@RunWith(MockitoJUnitRunner.class)
public class ImportActionTest extends BaseTest {

  @Test
  public void test_import() throws Exception {

    ImportAction command = new ImportAction();
    inject(command, asList(diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService, stopService, nomadEntityProvider, outputService, jsonFactory, json));
    command.setConfigFile(Paths.get(getClass().getResource("/my-cluster.cfg").toURI()));
    command.run();

    int[] ports = {9411, 9421, 9422};
    IntStream.of(ports).forEach(rethrow(port -> {
      verify(dynamicConfigServiceMock("localhost", port), times(1)).setUpcomingCluster(any());
    }));
  }
}
