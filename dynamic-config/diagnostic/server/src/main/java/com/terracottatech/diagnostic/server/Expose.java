/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to determine if a diagnostic service will be exposed or not through JMX.
 * <p>
 * By default, diagnostic services are private and not expose so this annotation is necessary
 * to get a service exposed.
 * <p>
 * Note: whether a diagnostic service is exposed or not through JMX, we will still have access
 * to it through the diagnostic service proxy mechanism.
 * <p>
 * This annotation only determines of the diagnostic service will also be made available in a
 * JMX console
 * <p>
 * You can also expose a service that is not annotated with {@link DiagnosticServicesRegistration}
 *
 * @author Mathieu Carbou
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Expose {
  /**
   * Mbean name
   */
  String value();
}
