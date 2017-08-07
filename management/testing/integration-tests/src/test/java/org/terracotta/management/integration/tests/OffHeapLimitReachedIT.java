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
package org.terracotta.management.integration.tests;

import org.junit.Test;
import org.terracotta.management.model.notification.ContextualNotification;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class OffHeapLimitReachedIT extends AbstractSingleTest {

  @Test
  public void notification_sent_when_offheap_limit_reached() throws Exception {
    getCaches("another1");
    getCaches("another2");

    List<ContextualNotification> notifications = waitForAllNotifications("OFFHEAP_RESOURCE_THRESHOLD_REACHED");
    Map<String, String> attributes = notifications.stream()
        .filter(n -> n.getType().equals("OFFHEAP_RESOURCE_THRESHOLD_REACHED"))
        .findFirst()
        .get()
        .getAttributes();
    assertThat(attributes.keySet(), hasItem("oldThreshold"));
    assertThat(attributes.keySet(), hasItem("threshold"));
    assertThat(attributes.keySet(), hasItem("capacity"));
    assertThat(attributes.keySet(), hasItem("available"));
    
    assertThat(attributes.get("oldThreshold"), equalTo("0"));
    assertThat(attributes.get("threshold"), equalTo("75"));

    getCaches("another3");

    notifications = waitForAllNotifications("OFFHEAP_RESOURCE_THRESHOLD_REACHED");
    attributes = notifications.stream()
        .filter(n -> n.getType().equals("OFFHEAP_RESOURCE_THRESHOLD_REACHED"))
        .findFirst()
        .get()
        .getAttributes();
    
    assertThat(attributes.get("oldThreshold"), equalTo("75"));
    assertThat(attributes.get("threshold"), equalTo("90"));

    destroyCaches("another3");

    notifications = waitForAllNotifications("OFFHEAP_RESOURCE_THRESHOLD_REACHED");
    attributes = notifications.stream()
        .filter(n -> n.getType().equals("OFFHEAP_RESOURCE_THRESHOLD_REACHED"))
        .findFirst()
        .get()
        .getAttributes();
    
    assertThat(attributes.get("oldThreshold"), equalTo("90"));
    assertThat(attributes.get("threshold"), equalTo("75"));

    destroyCaches("another2");
    destroyCaches("another1");

    notifications = waitForAllNotifications("OFFHEAP_RESOURCE_THRESHOLD_REACHED");
    attributes = notifications.stream()
        .filter(n -> n.getType().equals("OFFHEAP_RESOURCE_THRESHOLD_REACHED"))
        .findFirst()
        .get()
        .getAttributes();
    
    assertThat(attributes.get("oldThreshold"), equalTo("75"));
    assertThat(attributes.get("threshold"), equalTo("0"));
  }

}
