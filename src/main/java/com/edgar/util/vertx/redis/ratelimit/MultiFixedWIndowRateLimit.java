package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.AbstractLuaEvaluator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.redis.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by edgar on 17-5-29.
 */
public class MultiFixedWIndowRateLimit extends AbstractLuaEvaluator {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiFixedWIndowRateLimit.class);


  public MultiFixedWIndowRateLimit(Vertx vertx, RedisClient redisClient, Future<Void> completed) {
    super(vertx, redisClient, "multi_fixed_window_ratelimit.lua", completed);
  }

  /**
   * 限流
   *
   * @param limits  限流集合，必须包含三个元素:subject，limit,interval
   * @param handler 　回调
   */
  public void rateLimit(List<FixedWindowRateLimitOptions> limits, Handler<AsyncResult<RateLimitResult>> handler) {
    JsonArray limitArray;
    try {
      limitArray = checkArgument(limits);
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
      return;
    }
    List<String> keys = new ArrayList<>();
    List<String> args = new ArrayList<>();
    args.add(limitArray.encode());
    args.add(Instant.now().getEpochSecond() + "");
    evaluate(keys, args, ar -> {
      if (ar.failed()) {
        LOGGER.error("rateLimit failed", ar.cause());
        handler.handle(Future.failedFuture("rateLimit failed"));
        return;
      }
      RateLimitUtils.create(ar.result(), handler);
    });
  }

  private JsonArray checkArgument(List<FixedWindowRateLimitOptions> limits) {
    if (limits.size() == 0) {
      throw new IllegalArgumentException("limits cannot empty");
    }
    JsonArray limitArray = new JsonArray();
    for (int i = 0; i < limits.size(); i++) {
      FixedWindowRateLimitOptions limit = limits.get(i);
      try {
        limitArray.add(new JsonArray().add(limit.getSubject())
            .add(limit.getLimit())
            .add(limit.getInterval()));
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    return limitArray;
  }

}
