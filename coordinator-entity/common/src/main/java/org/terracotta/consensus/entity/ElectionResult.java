/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus.entity;

/**
 *
 * @author cdennis
 */
public enum ElectionResult implements ElectionResponse {
  
  NOT_ELECTED {
    public boolean isPending() {
      return false;
    }
  }, PENDING {
    public boolean isPending() {
      return true;
    }
  };

}
