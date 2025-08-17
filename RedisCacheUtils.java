package jimmy.practice.spd.component.user;

import cn.hutool.core.lang.Assert;
import jimmy.practice.basic.common.concurrent.lock.LockUtils;
import jimmy.practice.basic.common.exception.BizException;
import lombok.*;
import lombok.Builder.Default;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <pre>
 * 功能：实现了从Redis缓存中获取数据，如果缓存中没有就去其他数据源加载最新数据然后放到缓存中(computeIfAbsent)。主要特性是解决了『缓存击穿』和『缓存雪崩』的问题。
 * 缓存击穿: 一个热点key失效后，同一时间大量请求到来，大量请求都判断到缓存不存在而去请求其他数据源加载数据。
 * 缓存雪崩: 大量key同时失效，同一时间大量请求到来，分别请求这些已经失效的key, 每个key的请求量可能不大，但是key多，最终能触发其他数据源加载数据的请求就很多。
 *  其实关键点是『大量key在缓存中没有』，同时有大量请求请求这些key。可以导致『大量key在缓存中没有』的原因：
 *  1.同一时间大量key失效（这只是其中一种可能，平时常说的给过期时间加一个随机值只针对这个场景）
 *  2.一段时间系统基本没有流量，大量key逐渐失效(并不像1那样短时间内失效，有一个比较长的过程)
 *  3.手动把缓存全清了也是一种可能
 *
 * 缓存击穿解决方案
 * 加排他锁，保证只有一个线程可以加载数据，其他线程阻塞等待，等待超时可以触发降级逻辑或者直接失败。
 *
 * 缓存雪崩解决方案
 * 1.缓存失效时间加上随机值，让key在同一时间失效的可能性降低。（只针对上面说的情况1)
 * 2.更为通用的方案
 * 方案一:通过资源池的方式，限制最多maxKeyToLoadData个不同的key进行加载数据
 *   优点: 这种场景下，资源池的模型比限流的模型要好
 *   缺点: 资源池用到类似Semaphore的数据结构，分成两个动作，获取许可(acquire)，释放许可(release)。难点在于分布式环境下，释放许可(release)的动作可能会失败，造成数据混乱。
 *   比如Redisson有提供分布式的Semaphore、CountDownLatch等结构，但其实不是100%靠谱。
 *   所以方案一并不是直接用类似Semaphore的数据结构，但思想上也属于资源池的范畴，也存在释放许可(release)失败的可能性，不过做了一些补偿/兜底
 * 方案二:通过限流的方式, 限制单位时间(1秒)内可以通过的不同的key的加载数据请求
 *   优点: RateLimiter只有一个动作, 获取许可(acquire)，无需考虑分布式环境下释放许可(release)失败的场景。
 *   缺点: 这种场景下，限流的模型不如资源池的模型好。
 *   比如下游服务最多只能同时处理100个请求，每个请求要处理1秒，限流QPS通过计算理应设成100。但万一出现下游服务卡顿了，处理时间变慢，RateLimiter的QPS不变，请求到下游服务的请求就会变多，超过下游服务的处理能力上限。
 * 其他话题：如果有需求缓存挂了，要直接访问数据库，不能直接报错保证高可用。基于上述方案，实现起来也简单。
 * </pre>
 *
 * @author jimmy
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheUtils {
    protected static final String KEY_PREFIX = RedisCacheUtils.class.getSimpleName() + ":";
    protected static final String SYSTEM_BUSY = "系统繁忙, 请稍后再试";
    protected final LockUtils lockUtils;
    protected final Redisson redisson;

    public <T> T computeIfAbsent(String key, long ttl, TimeUnit ttlUnit,
                                 Supplier<T> loadDataLogic) {
        return computeIfAbsent(key, ttl, ttlUnit,
                loadDataLogic, ControlOptions.builder().build());
    }

    public <T> T computeIfAbsent(String key, long ttl, TimeUnit ttlUnit,
                                 Supplier<T> loadDataLogic,
                                 ControlOptions controlOptions) {
        return computeIfAbsent(key, ttl, ttlUnit,
                loadDataLogic, null, null,
                controlOptions);
    }

    public <T> T computeIfAbsent(String key, long ttl, TimeUnit ttlUnit,
                                 Supplier<T> loadDataLogic,
                                 BiConsumer<String, T> afterLoadDataLogic,
                                 Function<String, T> degradeLogic) {
        return computeIfAbsent(key, ttl, ttlUnit,
                loadDataLogic, afterLoadDataLogic, degradeLogic,
                ControlOptions.builder().build());
    }

    /**
     * 从缓存中取数据，取不到再去其他地方取。去其他地方取数据不能并发，只有一个线程，其他线程会阻塞等待那唯一的线程返回。
     * 等待超时可以选择进行降级，否则报错"系统繁忙, 请稍后再试"
     *
     * @param key                缓存key
     * @param ttl                缓存失效时间
     * @param ttlUnit            缓存失效时间单位
     * @param loadDataLogic      缓存key不存在的时候获取最新数据的逻辑
     * @param afterLoadDataLogic 加载到最新数据后的回调（可以把最新的数据存到本地供下次降级使用）
     * @param degradeLogic       缓存key不存在时，可能会有多个线程同时尝试加载最新数据，只有一个线程能加载数据，其余线程需要阻塞等待，等待超时会触发降级逻辑
     *                           如果没有降级逻辑将会抛出异常"系统繁忙, 请稍后再试"。
     * @param controlOptions     执行过程中的一些控制选项
     */
    public <T> T computeIfAbsent(String key, long ttl, TimeUnit ttlUnit,
                                 Supplier<T> loadDataLogic,
                                 BiConsumer<String, T> afterLoadDataLogic,
                                 Function<String, T> degradeLogic,
                                 ControlOptions controlOptions) {
        Assert.notNull(controlOptions, "controlOptions不能为空");
        RBucket<Object> bucket = redisson.getBucket(key);
        // 从Redis缓存中读取数据
        T result = (T) bucket.get();
        if (result != null) {
            if (controlOptions.cacheAvalancheBySemaphore()) {
                /*
                 * 【缓存雪崩】解决方案一:通过资源池 的补偿/兜底动作
                 * nowLoadingKeys集合表示正在进行数据加载的key，集合的最大数量不能超过controlOptions.maxKeyToLoadData，从而限制同时进行数据加载线程的数量
                 * 触发数据加载之前会把key添加到nowLoadingKeys集合，加载数据之后就会从nowLoadingKeys集合删除key
                 * 关键点在于『从nowLoadingKeys集合删除key』这个动作可能会失败(比如网络原因)，可能会没执行(比如服务挂了没办法执行)，这时候就会出现明明这个key没有在加载数据，但却一直在集合中(占用着一个许可资源)的情况
                 *
                 * 这里就是为了解决这种情况的补偿/兜底动作
                 * result!=null进到这里表示key在缓存中有数据，nowLoadingKeys集合正常是不应该有这个key的，假如有就是发生了上面说的特殊情况，这里补删一下。
                 * TODO: jimmy 2022/12/5 缺点：这样每次读缓存都多了一个删除动作，对性能有一定影响
                 */
                /*
                TODO: jimmy 2022/12/5 但这样每次读都多了一个写动作，虽然可以弄成异步删除，但还是每次读都产生了一次写操作；其实只要删过一次，在下次没命中缓存触发加载数据逻辑之前，一直重复删除是多余的
                解决方案
                    开始加载前(redis事务"原子性") {
                        loadingKeys.add(key)
                        set check_uuid {key: key, time: xxx}
                    }

                    加载完成后(redis事务"原子性") {
                        loadingKeys.remove(key)
                        delete check_uuid
                    }

                    定时任务
                    get checks
                    for dcheck_uuid in checks {
                        if 当前时间 - 30分钟 > check_uuid.time
                            // check_uuid.time很旧，有可能是下面两种情况，无法进行区分
                            // 1.很大可能是由于"加载完成后"的逻辑执行失败/服务挂了没法执行，导致一直留在缓存中
                            // 2.当然也可能是加载数据的逻辑还在执行中，还没执行"加载完成后"的逻辑，当然时间越旧可能性越低
                            修复动作(redis事务"原子性") {
                                if check_uuid.key在缓存中有值
                                    // 说明加载数据的线程早就已经把数据放到缓存中了，正常来说不应该存在check_uuid，现在check_uuid存在，原因只可能是"情况1"。修复一下loadingKeys
                                    loadingKeys.remove(check.key)
                                else
                                    // 修复一下loadingKeys，有可能有一个还在进行中的加载任务，它后面会从loadingKeys删掉对应的key，这里删掉有可能会提前增加了一个许可，增加了一种key能进行加载数据，突破了maxKeyToLoadData的上限(发生概率较低，可以接受)
                                    loadingKeys.remove(check.key)
                                // 释放掉check_uuid
                                delete check_uuid
                            }
                    }
                 */
                removeNowLoadingKey(key, controlOptions);
            }
            return result;
        }

        if (controlOptions.cacheAvalancheBySemaphore()) {
            //【缓存雪崩】解决方案一:通过资源池
            if (!addNowLoadingKey(key, controlOptions)) {
                // 往nowLoadingKeys集合添加准备要加载的key, 能加进去说明允许进行数据加载 (增强点，这里可以尝试做成阻塞等待而不是直接报错?)
                throw new BizException(SYSTEM_BUSY);
            }
        }

        return lockUtils.executeWithTryLock(KEY_PREFIX + key, () -> {
            try {
                // 再次从Redis缓存中读取数据(有可能多个线程同时竞争锁加载最新数据，线程1已经加载完了，后续线程拿到锁之后，如果缓存中已经有数据了就不必重复加载了
                T resultLoadByOtherThread = (T) bucket.get();
                if (resultLoadByOtherThread != null) {
                    return resultLoadByOtherThread;
                }
                if (controlOptions.cacheAvalancheByRateLimiter()) {
                    //【缓存雪崩】解决方案二:通过限流
                    acquireRateLimiter(controlOptions);
                }
                //【缓存击穿】解决方案: 拿到锁的线程(只可能有一个)去加载最新的数据
                T newestData = loadDataLogic.get();
                bucket.set(newestData, ttl, ttlUnit);
                // 加载到最新数据后的回调（可以把最新的数据存到本地供下次降级使用）
                if (afterLoadDataLogic != null) {
                    afterLoadDataLogic.accept(key, newestData);
                }
                return newestData;
            } finally {
                if (controlOptions.cacheAvalancheBySemaphore()) {
                    //【缓存雪崩】解决方案一:通过资源池 考虑这个操作失败/服务挂了没执行的可能性
                    removeNowLoadingKey(key, controlOptions);
                }
            }
        }, interrupted -> {
            // 等待超时，拿不到锁的线程进到这里
            if (degradeLogic != null) {
                // 执行降级逻辑（如果本地有缓存上次的数据可以用旧的数据）
                return degradeLogic.apply(key);
            } else {
                throw new BizException(SYSTEM_BUSY);
            }
        }, controlOptions.maxWaitTime, controlOptions.maxWaitTimeUnit);
    }

    private void acquireRateLimiter(ControlOptions controlOptions) {
        RRateLimiter rateLimiter = redisson.getRateLimiter(KEY_PREFIX + controlOptions.bizKey + ":RateLimiter");
        rateLimiter.setRate(RateType.OVERALL, controlOptions.qps, 1, RateIntervalUnit.SECONDS);
        rateLimiter.acquire();
    }

    private boolean addNowLoadingKey(String key, ControlOptions controlOptions) {
        RSet<String> nowLoadingKeys = getNowLoadingKeysSet(controlOptions);
        RLock lockForNowLoadingKeys = getNowLoadingKeysSetLock(controlOptions);
        return executeWithLock(lockForNowLoadingKeys, () -> {
            // 竞争条件，需要加锁，其他修改nowLoadingKeys集合的地方都要加锁
            int nowLoadingKeyCount = nowLoadingKeys.size();
            if (nowLoadingKeyCount >= controlOptions.maxKeyToLoadData) {
                return false;
            } else {
                return nowLoadingKeys.tryAdd(key);
            }
        });
    }

    private void removeNowLoadingKey(String key, ControlOptions controlOptions) {
        RSet<String> nowLoadingKeys = getNowLoadingKeysSet(controlOptions);
        RLock lockForNowLoadingKeys = getNowLoadingKeysSetLock(controlOptions);
        executeWithLock(lockForNowLoadingKeys, () -> nowLoadingKeys.removeAsync(key));
    }

    private RSet<String> getNowLoadingKeysSet(ControlOptions controlOptions) {
        return redisson.getSet(KEY_PREFIX + controlOptions.bizKey + ":NowLoadingKeys");
    }

    private RLock getNowLoadingKeysSetLock(ControlOptions controlOptions) {
        return redisson.getLock(KEY_PREFIX + controlOptions.bizKey + ":NowLoadingKeysLock");
    }

    private <T> T executeWithLock(Lock lock, Supplier<T> logic) {
        lock.lock();
        try {
            return logic.get();
        } finally {
            lock.unlock();
        }
    }


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ControlOptions {
        public static final int CACHE_AVALANCHE_BY_SEMAPHORE = 1;
        public static final int CACHE_AVALANCHE_BY_RATE_LIMITER = 2;
        /**
         * 缓存雪崩的解决方案
         * 1:通过资源池的方式，限制最多maxKeyToLoadData个不同的key进行加载数据
         * 2:通过限流的方式, 限制单位时间(1秒)内可以通过的不同的key的加载数据请求
         * 其他取值: 不解决
         */
        @Default
        private int cacheAvalancheSolution = 2;
        /**
         * <pre>
         * 业务标识，缓存雪崩的解决方案所需的一些统计信息按业务标识隔离
         * 比如采用方案一, 通过资源池
         *   不同业务标识, 可以设置不同的maxKeyToLoadData
         * 比如采用方案二, 通过限流
         *   不同业务标识，限流的速度qps可以不同
         * </pre>
         */
        @Default
        private String bizKey = "default";
        /**
         * 缓存雪崩的解决方案一: 通过资源池
         * 限制同时最多可以多少个不同的key进行数据加载
         */
        @Default
        private int maxKeyToLoadData = 1000;
        /**
         * 缓存雪崩的解决方案二: 通过限流
         * 限流的QPS
         */
        @Default
        private int qps = 500;
        /**
         * 执行加载数据逻辑前需要先获取锁，保证只有一个线程加载数据，获取锁最大的等待时间，等待超时会触发降级逻辑
         */
        @Default
        private int maxWaitTime = 3;
        /**
         * maxWaitTime的时间单位
         */
        @Default
        private TimeUnit maxWaitTimeUnit = TimeUnit.SECONDS;

        public boolean cacheAvalancheBySemaphore() {
            return this.cacheAvalancheSolution == ControlOptions.CACHE_AVALANCHE_BY_SEMAPHORE;
        }

        public boolean cacheAvalancheByRateLimiter() {
            return this.cacheAvalancheSolution == ControlOptions.CACHE_AVALANCHE_BY_RATE_LIMITER;
        }
    }
}
