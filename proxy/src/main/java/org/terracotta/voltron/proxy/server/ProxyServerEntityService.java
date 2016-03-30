package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.ProxyMessageCodec;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 * @author Mathieu Carbou
 */
public abstract class ProxyServerEntityService implements ServerEntityService<ProxyEntityMessage, ProxyEntityResponse> {

  private final Class<?> proxyType;
  private final Codec codec;
  private final Class<?>[] eventTypes;

  public ProxyServerEntityService(Class<?> proxyType) {
    this(proxyType, new SerializationCodec());
  }

  public ProxyServerEntityService(Class<?> proxyType, Codec codec, Class<?> ... eventTypes) {
    this.proxyType = proxyType;
    this.codec = codec;
    this.eventTypes = eventTypes;
  }

  @Override
  public ConcurrencyStrategy<ProxyEntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new NoConcurrencyStrategy<ProxyEntityMessage>();
  }

  @Override
  public MessageCodec<ProxyEntityMessage, ProxyEntityResponse> getMessageCodec() {
    return new ProxyMessageCodec(codec, proxyType, eventTypes);
  }

  @Override
  public PassiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> createPassiveEntity(final ServiceRegistry serviceRegistry, final byte[] bytes) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public SyncMessageCodec<ProxyEntityMessage, ProxyEntityResponse> getSyncMessageCodec() {
    return null;
  }
}
