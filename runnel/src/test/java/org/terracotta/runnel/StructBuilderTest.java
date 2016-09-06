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
package org.terracotta.runnel;

import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class StructBuilderTest {

  @Test(expected = IllegalArgumentException.class)
  public void checkIndexZeroIsInvalid() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkNegativeIndexIsInvalid() throws Exception {
    StructBuilder.newStructBuilder().int32("a", -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkDuplicateIndexIsInvalid() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 1).int32("b", 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkShrinkingIndexIsInvalid() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 2).int32("b", 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkDuplicateNameIsInvalid() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 1).int32("a", 2);
  }

  @Test
  public void checkValidConfigWorks() throws Exception {
    StructBuilder.newStructBuilder().int32("a", 1).int32("b", 2);
  }

}
