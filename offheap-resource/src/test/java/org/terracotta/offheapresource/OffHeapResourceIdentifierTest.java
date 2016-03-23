/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */
package org.terracotta.offheapresource;

import static org.hamcrest.core.Is.is;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class OffHeapResourceIdentifierTest {

  @Test
  public void testNullName() {
    try {
      OffHeapResourceIdentifier.identifier(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      //expected
    }
  }

  @Test
  public void testServiceType() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").getServiceType(), equalTo(OffHeapResource.class));
  }

  @Test
  public void testEquals() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").equals(OffHeapResourceIdentifier.identifier("foo")), is(true));
    assertThat(OffHeapResourceIdentifier.identifier("foo").equals(OffHeapResourceIdentifier.identifier("bar")), is(false));
  }

  @Test
  public void testHashcode() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").hashCode(), is(OffHeapResourceIdentifier.identifier("foo").hashCode()));
  }
}
