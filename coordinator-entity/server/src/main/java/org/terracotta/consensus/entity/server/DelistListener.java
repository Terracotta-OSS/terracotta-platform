package org.terracotta.consensus.entity.server;


public interface DelistListener<K, V> {
  
  void onDelist(K key, V value, Object permit);

}
