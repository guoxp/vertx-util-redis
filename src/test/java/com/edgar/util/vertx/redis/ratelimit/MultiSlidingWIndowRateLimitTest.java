package com.edgar.util.vertx.redis.ratelimit;

import com.edgar.util.vertx.redis.RedisDeletePattern;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by edgar on 17-5-28.
 */
@RunWith(VertxUnitRunner.class)
public class MultiSlidingWIndowRateLimitTest {

  private RedisClient redisClient;

  private Vertx vertx;

  @Before
  public void setUp() {
    vertx = Vertx.vertx();
    redisClient = RedisClient.create(vertx, new RedisOptions()
    .setHost("127.0.0.1"));
    AtomicBoolean complete = new AtomicBoolean();
    RedisDeletePattern.create(redisClient)
        .deleteByPattern("rate.limit*", ar -> {complete.set(true);});
    Awaitility.await().until(() -> complete.get());
  }

  @Test
  public void testRateLimit3ReqPer5sWith1sPrecision(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    MultSlidingWIndowRateLimit rateLimit = new MultSlidingWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<RateLimitResponse> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    SlidingWindowRateLimitOptions options =
        new SlidingWindowRateLimitOptions(subject).setLimit(3).setInterval(5)
            .setPrecision(1);
    List<SlidingWindowRateLimitOptions> list = new ArrayList<>();
    list.add(options);
    for (int i = 0; i < 6; i ++) {
      rateLimit.rateLimit(list, ar -> {
        if (ar.failed()) {
          testContext.fail();
        } else {
          req.incrementAndGet();
          result.add(ar.result());
        }
      });
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Awaitility.await().until(() -> req.get() == 6);
    System.out.println(result);

    Assert.assertEquals(4, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(1, result.get(0).resetSeconds());
    Assert.assertEquals(2, result.get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(1, result.get(1).resetSeconds());
    Assert.assertEquals(1, result.get(1).remaining());
    Assert.assertTrue(result.get(1).passed());

    Assert.assertEquals(1, result.get(2).resetSeconds());
    Assert.assertEquals(0, result.get(2).remaining());
    Assert.assertTrue(result.get(2).passed());

    Assert.assertEquals(1, result.get(3).resetSeconds());
    Assert.assertEquals(0, result.get(3).remaining());
    Assert.assertFalse(result.get(3).passed());

    Assert.assertEquals(1, result.get(4).resetSeconds());
    Assert.assertEquals(0, result.get(4).remaining());
    Assert.assertFalse(result.get(4).passed());

    Assert.assertEquals(1, result.get(5).resetSeconds());
    Assert.assertEquals(0, result.get(5).remaining());
    Assert.assertTrue(result.get(5).passed());
  }

  @Test
  public void testRateLimit3ReqPer5sWith5sPrecision(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    MultSlidingWIndowRateLimit rateLimit = new MultSlidingWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<RateLimitResponse> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    SlidingWindowRateLimitOptions options =
        new SlidingWindowRateLimitOptions(subject).setLimit(3).setInterval(5)
            .setPrecision(5);
    List<SlidingWindowRateLimitOptions> list = new ArrayList<>();
    list.add(options);
    for (int i = 0; i < 14; i ++) {
      rateLimit.rateLimit(list, ar -> {
        if (ar.failed()) {
          testContext.fail();
        } else {
          req.incrementAndGet();
          result.add(ar.result());
        }
      });
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Awaitility.await().until(() -> req.get() == 14);
    System.out.println(result);

    Assert.assertEquals(9, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(5, result.get(0).resetSeconds());
    Assert.assertEquals(2, result.get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(4, result.get(1).resetSeconds());
    Assert.assertEquals(1, result.get(1).remaining());
    Assert.assertTrue(result.get(1).passed());

    Assert.assertEquals(3, result.get(2).resetSeconds());
    Assert.assertEquals(0, result.get(2).remaining());
    Assert.assertTrue(result.get(2).passed());

    Assert.assertEquals(2, result.get(3).resetSeconds());
    Assert.assertEquals(0, result.get(3).remaining());
    Assert.assertFalse(result.get(3).passed());

    Assert.assertEquals(1, result.get(4).resetSeconds());
    Assert.assertEquals(0, result.get(4).remaining());
    Assert.assertFalse(result.get(4).passed());

    Assert.assertEquals(5, result.get(5).resetSeconds());
    Assert.assertEquals(2, result.get(5).remaining());
    Assert.assertTrue(result.get(5).passed());
  }

  @Test
  public void testTwoRateLimit(TestContext testContext) {
    AtomicBoolean complete = new AtomicBoolean();
    Future<Void> future = Future.future();
    future.setHandler(ar -> {
      if (ar.succeeded()) {
        complete.set(true);
      } else {
        complete.set(false);
      }
    });
    MultSlidingWIndowRateLimit rateLimit = new MultSlidingWIndowRateLimit(vertx, redisClient, future);
    Awaitility.await().until(() -> complete.get());
    AtomicInteger req = new AtomicInteger();
    List<RateLimitResponse> result = new ArrayList<>();
    String subject = UUID.randomUUID().toString();
    SlidingWindowRateLimitOptions options =
        new SlidingWindowRateLimitOptions(subject).setLimit(5).setInterval(10)
            .setPrecision(2);
    SlidingWindowRateLimitOptions options2 =
        new SlidingWindowRateLimitOptions(subject).setLimit(3).setInterval(5)
            .setPrecision(1);
    List<SlidingWindowRateLimitOptions> list = new ArrayList<>();
    list.add(options);
    list.add(options2);
    for (int i = 0; i < 14; i ++) {
      rateLimit.rateLimit(list, ar -> {
        if (ar.failed()) {
          testContext.fail();
        } else {
          req.incrementAndGet();
          result.add(ar.result());
        }
      });
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Awaitility.await().until(() -> req.get() == 14);
    System.out.println(result);

    Assert.assertEquals(8, result.stream().filter(resp -> resp.passed()).count());
    Assert.assertEquals(2, result.get(0).resetSeconds());
    Assert.assertEquals(4, result.get(0).remaining());
    Assert.assertTrue(result.get(0).passed());

    Assert.assertEquals(1, result.get(1).resetSeconds());
    Assert.assertEquals(3, result.get(1).remaining());
    Assert.assertTrue(result.get(1).passed());

    Assert.assertEquals(2, result.get(2).resetSeconds());
    Assert.assertEquals(2, result.get(2).remaining());
    Assert.assertTrue(result.get(2).passed());

    Assert.assertEquals(1, result.get(3).resetSeconds());
    Assert.assertEquals(0, result.get(3).remaining());
    Assert.assertFalse(result.get(3).passed());

    Assert.assertEquals(1, result.get(4).resetSeconds());
    Assert.assertEquals(0, result.get(4).remaining());
    Assert.assertFalse(result.get(4).passed());

    Assert.assertEquals(1, result.get(5).resetSeconds());
    Assert.assertEquals(1, result.get(5).remaining());
    Assert.assertTrue(result.get(5).passed());
  }

}
