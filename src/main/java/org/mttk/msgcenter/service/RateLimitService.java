package org.mttk.msgcenter.service;



public interface RateLimitService {
    boolean isRequestAllowed(String sourceId,int channel,boolean isTimerMsg);
}
