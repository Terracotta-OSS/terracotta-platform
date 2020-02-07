/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.common;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.json.Json;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class NomadMessageCodec implements MessageCodec<NomadEntityMessage, NomadEntityResponse> {
  @Override
  public byte[] encodeMessage(NomadEntityMessage message) throws MessageCodecException {
    try {
      return Json.toJson(message).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
    try {
      return Json.parse(new String(payload, UTF_8), NomadEntityMessage.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] encodeResponse(NomadEntityResponse response) throws MessageCodecException {
    try {
      return Json.toJson(response).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
    try {
      return Json.parse(new String(payload, UTF_8), NomadEntityResponse.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }
}
