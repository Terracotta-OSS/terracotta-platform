/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

/**
 * @author Mathieu Carbou
 */
public abstract class DiagnosticCodecSkeleton<E> implements DiagnosticCodec<E> {
  private final Class<E> encodedType;

  public DiagnosticCodecSkeleton(Class<E> encodedType) {
    this.encodedType = encodedType;
  }

  @Override
  public Class<E> getEncodedType() {
    return encodedType;
  }
}
