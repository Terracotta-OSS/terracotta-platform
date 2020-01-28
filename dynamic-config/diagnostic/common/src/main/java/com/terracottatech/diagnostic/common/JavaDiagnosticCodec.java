/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class JavaDiagnosticCodec extends DiagnosticCodecSkeleton<byte[]> {
  public JavaDiagnosticCodec() {
    super(byte[].class);
  }

  @Override
  public byte[] serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
      oos.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public <T> T deserialize(byte[] encoded, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(encoded);
    requireNonNull(target);
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(encoded))) {
      return target.cast(ois.readObject());
    } catch (Exception e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public String toString() {
    return "Java";
  }
}
