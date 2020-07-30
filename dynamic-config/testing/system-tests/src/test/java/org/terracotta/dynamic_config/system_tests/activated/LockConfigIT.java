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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.common.struct.LockContext;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;

@ClusterDefinition(autoActivate = true)
public class LockConfigIT extends DynamicConfigIT {
  private final LockContext lockContext =
      new LockContext(UUID.randomUUID().toString(), "platform", "dynamic-scale");

  @Test
  public void testLockUnlock() {
    lock();
    unlock();
  }

  @Test
  public void testSettingChangesWithoutTokenWhileLocked() {
    lock();

    assertThat(
        () -> invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB"),
        exceptionMatcher("changes are not allowed as config is locked by 'platform (dynamic-scale)'")
    );

    unlock();

    invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB");
  }

  @Test
  public void testSettingChangesWithTokenWhileLocked() {
    lock();

    invokeWithToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB");
  }

  private void lock() {
    invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "lock-context=" + lockContext);
  }

  private void unlock() {
    invokeWithToken("unset", "-s", "localhost:" + getNodePort(), "-c", "lock-context");
  }

  private void invokeWithoutToken(String... args) {
    invokeConfigTool(args);
  }

  private void invokeWithToken(String... args) {
    List<String> newArgs = new ArrayList<>(asList("--lock-token", lockContext.getToken()));
    newArgs.addAll(asList(args));
    invokeConfigTool(newArgs.toArray(new String[0]));
  }
}
