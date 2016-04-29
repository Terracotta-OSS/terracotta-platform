package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.MessageCodec;
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
public abstract class ProxyServerEntityService<C> implements ServerEntityService<ProxyEntityMessage, ProxyEntityResponse> {

  private final Class<?> proxyType;
  private final Codec codec;
  private final Class<?>[] eventTypes;
  private final Class<C> configType;

  public ProxyServerEntityService(Class<?> proxyType, Class<C> configType) {
    this(proxyType, configType, new SerializationCodec());
  }

  public ProxyServerEntityService(Class<?> proxyType, Class<C> configType, Codec codec, Class<?> ... eventTypes) {
    this.proxyType = proxyType;
    this.configType = configType;
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
  public SyncMessageCodec<ProxyEntityMessage> getSyncMessageCodec() {
    return null;
  }

  @Override
  public ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> createActiveEntity(ServiceRegistry registry, byte[] bytes) {
    C config = null;
    if (configType == Void.TYPE) {
      if (bytes != null && bytes.length > 0) {
        throw new IllegalArgumentException("No config expected here!");
      }
    } else {
      config = configType.cast(codec.decode(bytes, configType));
    }
    return createActiveEntity(registry, config);
  }

  protected ActiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> createActiveEntity(ServiceRegistry registry, C configuration) {
    throw new UnsupportedOperationException("Implement me!");
  }

}
