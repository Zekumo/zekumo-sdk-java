package io.zekumo.sdk;
public final class ZekumoException extends RuntimeException {
  public final int status; public final String code;
  public ZekumoException(int status,String code,String message){super(message);this.status=status;this.code=code;}
}
