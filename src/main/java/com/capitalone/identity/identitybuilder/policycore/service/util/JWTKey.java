package com.capitalone.identity.identitybuilder.policycore.service.util;

import java.time.Instant;
import java.util.List;

import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class JWTKey {
  private static final Logger logger = LogManager.getLogger(JWTKey.class);

  private String productId;
  private String exp;
  private String kid;
  private String kty;
  private String n;
  private String e;
  private String crv;
  private String x;
  private String y;
  private String use;
  private List<String> key_ops;
  private String alg;

  public boolean isExpired() {
    Instant expiration = Instant.ofEpochSecond(Long.parseLong(exp));
    logger.info("expiration time : {} for the product id : {}", this.exp, this.productId);
    logger.info("Expired check result: {} ", Instant.now().isAfter(expiration));
    return Instant.now().isAfter(expiration);
  }

}
