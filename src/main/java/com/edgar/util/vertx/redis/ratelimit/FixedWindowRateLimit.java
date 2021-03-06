package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.AbstractLuaEvaluator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class FixedWindowRateLimit extends AbstractLuaEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(FixedWindowRateLimit.class);


  public FixedWindowRateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    super(vertx, redisClient, "fixed_window_ratelimit.lua", completed);
  }

  /**
   * 限流
   * @param rateLimit 限流设置
   * @param handler　回调
   */
  public void rateLimit(FixedWindowRateLimitRule rateLimit, Handler<AsyncResult<LimitResult>> handler) {
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(rateLimit.getSubject());
    args.add(rateLimit.getLimit() + "");
    args.add(rateLimit.getInterval() + "");
    args.add(Instant.now().getEpochSecond() + "");
    evaluate(keys, args, ar -> {
      if (ar.failed()) {
        LOGGER.error("rateLimit failed", ar.cause());
        handler.handle(Future.failedFuture("rateLimit failed"));;
        return;
      }
      List<String> subjects = new ArrayList<>();
      subjects.add(rateLimit.getSubject());
      RateLimitUtils.createResult(ar.result(), subjects, handler);
    });
  }

}
