package org.smartboot.socket.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

public class HashedWheelTimer implements Timer, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HashedWheelTimer.class);

    /**
     * 指针波动频率
     */
    private final long tickDuration;
    /**
     * 时间轮槽位数
     */
    private final HashedWheelBucket[] wheel;
    private final int mask;
    /**
     * 新注册的定时任务
     */
    private final Queue<HashedWheelTimerTask> newTimeouts = new ConcurrentLinkedQueue<>();
    /**
     * 已取消的定时任务
     */
    private final Queue<HashedWheelTimerTask> cancelledTimeouts = new ConcurrentLinkedQueue<>();
    /**
     * 待处理任务数
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);

    /**
     * 定时器启动时间
     */
    private volatile long startTime;

    private boolean running = true;

    public static final HashedWheelTimer DEFAULT_TIMER = new HashedWheelTimer(r -> new Thread(r, "defaultHashedWheelTimer"));

    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, 512);
    }

    /**
     * @param threadFactory
     * @param tickDuration  波动周期,单位：毫秒
     * @param ticksPerWheel 时间轮大小,自适应成 2^n
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, int ticksPerWheel) {
        // 创建长度为2^n大小的时间轮
        wheel = createWheel(ticksPerWheel);
        mask = wheel.length - 1;
        this.tickDuration = TimeUnit.MILLISECONDS.toNanos(tickDuration);
        Thread workerThread = threadFactory.newThread(this);
        workerThread.start();
    }

    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int n = ticksPerWheel - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1073741824) ? 1073741824 : n + 1;
    }


    @Override
    public void shutdown() {
        running = false;
    }

    public TimerTask scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit unit) {
        return schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {
                    schedule(this, delay, unit);
                }
            }
        }, delay, unit);
    }

    @Override
    public TimerTask schedule(Runnable runnable, long delay, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        if (deadline <= 0) {
            throw new IllegalArgumentException();
        }
        pendingTimeouts.incrementAndGet();
        HashedWheelTimerTask timeout = new HashedWheelTimerTask(this, runnable, deadline);
        newTimeouts.add(timeout);
        return timeout;
    }

    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    private long tick;

    @Override
    public void run() {
        startTime = System.nanoTime();
        while (running) {
            final long deadline = waitForNextTick();
            //移除已取消的任务
            processCancelledTasks();
            //将新任务分配至各分桶
            transferTimeoutsToBuckets();
            wheel[(int) (tick & mask)].execute(deadline);
            tick++;
        }
    }

    private void transferTimeoutsToBuckets() {
        //限制注册的个数，防止因此导致其他任务延迟
        for (int i = 0; i < 100000; i++) {
            HashedWheelTimerTask timeout = newTimeouts.poll();
            if (timeout == null) {
                break;
            }
            if (timeout.state() == HashedWheelTimerTask.ST_CANCELLED) {
                continue;
            }

            long calculated = timeout.deadline / tickDuration;
            timeout.remainingRounds = (calculated - tick) / wheel.length;

            final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
            int stopIndex = (int) (ticks & mask);

            HashedWheelBucket bucket = wheel[stopIndex];
            bucket.addTimeout(timeout);
        }
    }

    /**
     * 移除已取消的任务
     */
    private void processCancelledTasks() {
        HashedWheelTimerTask timeout;
        while ((timeout = cancelledTimeouts.poll()) != null) {
            timeout.remove();
        }
    }

    private long waitForNextTick() {
        long deadline = tickDuration * (tick + 1);

        while (true) {
            long currentTime = System.nanoTime() - startTime;
            //时间对齐
            long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

            if (sleepTimeMs <= 0) {
                if (currentTime <= 0) {
                    LOG.warn("System.nanoTime() is overflow");
                    return waitForNextTick();
                } else {
                    return currentTime;
                }
            }

            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class HashedWheelTimerTask implements TimerTask {

        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;
        private static final AtomicIntegerFieldUpdater<HashedWheelTimerTask> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimerTask.class, "state");

        private final HashedWheelTimer timer;
        private final Runnable runnable;

        private final long deadline;

        private volatile int state = ST_INIT;

        // 剩余的轮训圈数
        long remainingRounds;

        HashedWheelTimerTask next;
        HashedWheelTimerTask prev;

        HashedWheelBucket bucket;

        HashedWheelTimerTask(HashedWheelTimer timer, Runnable runnable, long deadline) {
            this.timer = timer;
            this.runnable = runnable;
            this.deadline = deadline;
        }

        @Override
        public boolean cancel() {
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            timer.cancelledTimeouts.add(this);
            return true;
        }

        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean isDone() {
            return state() == ST_EXPIRED;
        }

        public void execute() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }

            try {
                runnable.run();
            } catch (Throwable t) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("An exception was thrown", t);
                }
            }
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;

            StringBuilder buf = new StringBuilder(192).append(getClass().getSimpleName()).append('(').append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining).append(" ns ago");
            } else {
                buf.append("now");
            }

            if (isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(", task: ").append(runnable).append(')').toString();
        }
    }

    /**
     * 定时器分桶
     */
    private static final class HashedWheelBucket {
        private HashedWheelTimerTask head;
        private HashedWheelTimerTask tail;

        public void addTimeout(HashedWheelTimerTask timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        public void execute(long deadline) {
            HashedWheelTimerTask timeout = head;
            while (timeout != null) {
                HashedWheelTimerTask next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        timeout.execute();
                    } else {
                        throw new IllegalStateException();
                    }
                } else if (timeout.isCancelled()) {
                    next = remove(timeout);
                } else {
                    timeout.remainingRounds--;
                }
                timeout = next;
            }
        }

        public HashedWheelTimerTask remove(HashedWheelTimerTask timeout) {
            HashedWheelTimerTask next = timeout.next;
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }

            if (timeout == head) {
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                tail = timeout.prev;
            }
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }
    }
}