/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import org.junit.Test;
import org.w3c.dom.Element;

import com.terracottatech.dynamic_config.config.Measure;
import com.terracottatech.utilities.TimeUnit;

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