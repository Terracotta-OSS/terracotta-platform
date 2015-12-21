/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus.entity;

import java.io.Serializable;

/**
 *
 * @author cdennis
 */
public interface ElectionResponse extends Serializable {

  boolean isPending();
  
}
