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
package org.terracotta.state;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;


/**
 */
public class TransientEntityStateRepositoryTest {

  @Test
  public void testNewStateCreation() {
    TransientEntityStateRepository stateRepository = new TransientEntityStateRepository();

    Map<String, String> state = stateRepository.getOrCreateState("new", String.class, String.class);

    assertNotNull(state);

    Map<String, String> state1 = stateRepository.getOrCreateState("another", String.class, String.class);

    assertNotSame(state, state1);

  }

  @Test
  public void testExistingState() {
    TransientEntityStateRepository stateRepository = new TransientEntityStateRepository();

    Map<String, String> state = stateRepository.getOrCreateState("new", String.class, String.class);

    Map<String, String> newState = stateRepository.getOrCreateState("new", String.class, String.class);

    assertSame(state, newState);
  }

  @Test
  public void testDestroyState() {
    TransientEntityStateRepository stateRepository = new TransientEntityStateRepository();

    Map<String, String> state = stateRepository.getOrCreateState("new", String.class, String.class);

    stateRepository.destroyState("new");

    Map<String, String> state1 = stateRepository.getOrCreateState("new", String.class, String.class);

    assertNotSame(state, state1);
  }

}
