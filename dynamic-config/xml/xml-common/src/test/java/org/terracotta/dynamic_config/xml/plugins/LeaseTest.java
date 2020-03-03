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
package org.terracotta.dynamic_config.xml.plugins;

import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.w3c.dom.Element;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class LeaseTest {
  @Test
  public void testBasic() {
    Element element = new Lease(Measure.of(100, TimeUnit.MINUTES)).toElement();
    assertThat(element, notNullValue());

    Element leaseLength = (Element)element.getFirstChild();

    int actualTime = Integer.parseInt(leaseLength.getTextContent());
    assertThat(actualTime, is(100));

    TimeUnit actualUnit = TimeUnit.valueOf(leaseLength.getAttribute("unit").toUpperCase());
    assertThat(actualUnit, is(TimeUnit.MINUTES));
  }
}