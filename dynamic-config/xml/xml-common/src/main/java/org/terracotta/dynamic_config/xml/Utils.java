/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.dom.DOMResult;

public class Utils {
  public static <T> Element createElement(JAXBElement<T> jaxbElement) {
    DOMResult res = new DOMResult();

    JAXB.marshal(jaxbElement, res);

    return ((Document)res.getNode()).getDocumentElement();
  }
}
