package org.smartboot.socket.timer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于时间轮算法实现的高效定时器
 * <p>
 * 时间轮算法是一种高效的定时任务调度算法，适用于大量定时任务的场景。
 * 其核心思想是将时间分割成等长的时间槽(slot)，每个槽位对应一个任务链表，
 * 通过时间轮的旋转来触发定时任务的执行。
 * </p>
 * <p>
 * 该实现具有以下特点：
 * 1. 时间复杂度：添加任务和删除任务的时间复杂度为O(1)
 * 2. 支持一次性定时任务和固定延迟的周期性任务
 * 3. 使用哈希算法将任务分配到不同的时间槽，提高并发性能
 * </p>
 */
public class HashedWheelTimer implements Timer, Runnable {

    /**
     * 指针波动频率，即时间轮每次转动的时间间隔（单位：毫秒）
     * 该值决定了定时任务的最小精度，值越小精度越高，但系统开销也越大
     */
    private final long tickDuration;
    
    /**
     * 时间轮数组，每个元素是一个HashedWheelBucket（任务桶），存储对应槽位的定时任务
     * 时间轮的大小会被调整为2的幂，以便使用位运算进行高效的取模操作
     */
    private final HashedWheelBucket[] wheel;
    
    /**
     * 掩码值，用于计算任务应该放入的槽位索引
     * 由于wheel长度是2的幂，mask = wheel.length - 1，可以用位运算代替取模操作
     */
    private final int mask;
    
    /**
     * 新注册的定时任务队列
     * 使用ConcurrentLinkedQueue保证线程安全，新任务先放入此队列，再由工作线程分配到对应的时间轮槽位
     */
    private final Queue<HashedWheelTimerTask> newTimeouts = new ConcurrentLinkedQueue<>();
    
    /**
     * 已取消的定时任务队列
     * 当任务被取消时，会被加入此队列，由工作线程统一清理
     */
    private final Queue<HashedWheelTimerTask> cancelledTimeouts = new ConcurrentLinkedQueue<>();
    
    /**
     * 待处理任务数计数器
     * 用于统计当前定时器中的任务总数，包括新添加但尚未分配到时间轮的任务
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);

    /**
     * 定时器启动时间（毫秒）
     * 用作计算任务在时间轮中位置的基准时间点
     */
    private volatile long startTime;

    /**
     * 定时器运行状态标志
     * true表示定时器正在运行，false表示定时器已关闭
     */
    private volatile boolean running = true;

    /**
     * 默认的全局共享定时器实例
     * 使用守护线程运行，应用程序退出时会自动关闭
     * 适用于大多数场景，如果不需要特殊配置，推荐使用此实例
     */
    public static final HashedWheelTimer DEFAULT_TIMER = new HashedWheelTimer(r -> {
        Thread thread = new Thread(r, "defaultHashedWheelTimer");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 使用默认参数创建定时器
     * 默认时间精度为100毫秒，时间轮大小为512
     * 
     * @param threadFactory 线程工厂，用于创建定时器的工作线程
     */
    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, 512);
    }

    /**
     * 创建自定义参数的定时器
     * 
     * @param threadFactory 线程工厂，用于创建定时器的工作线程
     * @param tickDuration  指针波动周期，单位：毫秒，决定了定时任务的最小精度
     * @param ticksPerWheel 时间轮大小，会自适应调整为2^n，决定了时间轮的槽位数量
     *                      槽位数越多，任务在同一个槽位上的冲突概率越低，但内存占用也越大
     */
    public HashedWheelTimer(ThreadFactory threadFactory, long tickDuration, int ticksPerWheel) {
        // 创建长度为2^n大小的时间轮
        wheel = createWheel(ticksPerWheel);
        // 计算掩码，用于快速定位槽位
        mask = wheel.length - 1;
        this.tickDuration = tickDuration;
        // 创建并启动工作线程
        Thread workerThread = threadFactory.newThread(this);
        workerThread.start();
    }

    /**
     * 创建时间轮数组
     * 
     * @param ticksPerWheel 时间轮大小（槽位数）
     * @return 初始化后的时间轮数组，每个槽位包含一个HashedWheelBucket实例
     */
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        // 将输入的槽位数调整为2的幂
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        // 创建时间轮数组
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        // 初始化每个槽位
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 将输入的数值调整为最接近的2的幂
     * 例如：输入10，输出16；输入33，输出64
     * 这样可以使用位运算代替取模操作，提高性能
     * 
     * @param ticksPerWheel 原始槽位数
     * @return 调整后的槽位数（2的幂）
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        // 以下算法通过位运算将数字调整为2的幂
        int n = ticksPerWheel - 1;
        n |= n >>> 1;  // 将最高位1右边的一位设为1
        n |= n >>> 2;  // 将最高位1右边的两位设为1
        n |= n >>> 4;  // 将最高位1右边的四位设为1
        n |= n >>> 8;  // 将最高位1右边的八位设为1
        n |= n >>> 16; // 将最高位1右边的十六位设为1
        // 最终结果加1，得到2的幂
        // 同时处理边界情况：负数返回1，过大的数返回最大允许值
        return (n < 0) ? 1 : (n >= 1073741824) ? 1073741824 : n + 1;
    }


    @Override
    public void shutdown() {
        running = false;
    }

    /**
     * 安排一个固定延迟的周期性任务
     * 任务执行完成后，会在指定的延迟时间后再次执行，形成周期性执行的效果
     * 与固定频率不同，该方法是以上一次任务执行完成的时间点为基准计算下一次执行时间
     * 
     * @param runnable 要执行的任务
     * @param delay    延迟时间
     * @param unit     时间单位
     * @return 定时任务对象，可用于取消任务
     * @throws IllegalArgumentException 如果计算出的截止时间溢出（小于等于0）
     */
    public TimerTask scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(delay);
        if (deadline <= 0) {
            throw new IllegalArgumentException();
        }
        HashedWheelTimerTask timeout = new HashedWheelTimerTask(this, runnable, deadline);
        timeout.runnable = () -> {
            try {
                runnable.run();
            } finally {
                if (!timeout.isCancelled()) {
                    timeout.deadline = System.currentTimeMillis() + unit.toMillis(delay);
                    timeout.bucket = null;
                    timeout.next = null;
                    timeout.prev = null;
                    timeout.state = HashedWheelTimerTask.ST_INIT;
                    pendingTimeouts.incrementAndGet();
                    newTimeouts.add(timeout);
                }
            }
        };
        pendingTimeouts.incrementAndGet();
        newTimeouts.add(timeout);
        return timeout;
    }

    /**
     * 安排一个一次性定时任务
     * 任务将在指定的延迟时间后执行一次，然后结束
     * 
     * @param runnable 要执行的任务
     * @param delay    延迟时间
     * @param unit     时间单位
     * @return 定时任务对象，可用于取消任务
     * @throws IllegalArgumentException 如果计算出的截止时间溢出（小于等于0）
     */
    @Override
    public TimerTask schedule(Runnable runnable, long delay, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(delay);
        if (deadline <= 0) {
            throw new IllegalArgumentException();
        }
        HashedWheelTimerTask timeout = new HashedWheelTimerTask(this, runnable, deadline);
        pendingTimeouts.incrementAndGet();
        newTimeouts.add(timeout);
        return timeout;
    }

    /**
     * 获取当前待处理的任务数量
     * 包括已添加但尚未执行的任务总数
     * 
     * @return 待处理任务数量
     */
    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }

    /**
     * 当前时间轮的指针位置
     * 表示时间轮已经转动的次数，用于计算任务应该放入的槽位
     * 每次时间轮转动一格，tick值增加1
     */
    private long tick;

    /**
     * 定时器工作线程的主循环方法
     * 负责时间轮的转动和任务的调度执行
     * 该方法会在单独的线程中运行，直到定时器被关闭
     */
    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        while (running) {
            final long deadline = waitForNextTick();
            //移除已取消的任务
            processCancelledTasks();
            //将新任务分配至各分桶
            transferTimeoutsToBuckets();
            wheel[(int) (tick & mask)].execute(deadline, tickDuration);
            tick++;
        }
    }

    /**
     * 将新添加的任务分配到时间轮的对应槽位
     * 根据任务的截止时间计算应该放入的槽位索引
     * 为了防止单次处理任务过多导致其他任务延迟，每次最多处理100000个任务
     */
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

            long calculated = (timeout.deadline - startTime) / tickDuration;
            final long ticks = Math.max(calculated, tick);
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

    /**
     * 等待到下一个时间槽的时间点
     * 该方法会阻塞当前线程，直到达到下一个时间槽的时间点
     * 通过计算下一个时间点并使用Thread.sleep实现时间对齐
     * 
     * @return 当前时间（毫秒），用于后续任务执行的时间判断
     */
    private long waitForNextTick() {
        long deadline = startTime + tickDuration * (tick + 1);

        while (true) {
            //时间对齐
            long currentTime = System.currentTimeMillis();
            if (deadline <= currentTime) {
                return currentTime;
            }

            try {
                Thread.sleep(deadline - currentTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 哈希时间轮定时任务实现类
     * 表示一个待执行的定时任务，包含任务的状态、执行时间和关联的可运行对象
     * 使用双向链表结构在时间轮的槽位中组织任务
     */
    private static final class HashedWheelTimerTask implements TimerTask {

        /**
         * 任务初始状态
         * 表示任务已创建但尚未执行或取消
         */
        private static final int ST_INIT = 0;
        
        /**
         * 任务已取消状态
         * 表示任务已被用户取消，不会被执行
         */
        private static final int ST_CANCELLED = 1;
        
        /**
         * 任务已过期状态
         * 表示任务已经执行完毕
         */
        private static final int ST_EXPIRED = 2;
        /**
         * 任务状态原子更新器
         * 用于以线程安全的方式更新任务状态
         * 避免多线程环境下的竞态条件
         */
        private static final AtomicIntegerFieldUpdater<HashedWheelTimerTask> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimerTask.class, "state");

        /**
         * 关联的定时器实例
         * 用于在任务取消时通知定时器
         */
        private final HashedWheelTimer timer;
        
        /**
         * 任务执行的具体逻辑
         * 当任务到期时会调用此对象的run方法
         */
        private Runnable runnable;

        /**
         * 任务的截止时间（毫秒）
         * 当当前时间达到或超过此时间时，任务将被执行
         */
        private long deadline;

        /**
         * 任务当前状态
         * 使用volatile保证多线程可见性
         * 初始值为ST_INIT，表示任务已创建但尚未执行
         */
        private volatile int state = ST_INIT;

        /**
         * 双向链表中的下一个任务
         * 用于在同一个时间槽中链接多个任务
         */
        private HashedWheelTimerTask next;
        
        /**
         * 双向链表中的上一个任务
         * 用于在同一个时间槽中链接多个任务
         */
        private HashedWheelTimerTask prev;

        /**
         * 任务所在的时间槽（桶）
         * 指向包含此任务的HashedWheelBucket实例
         */
        private HashedWheelBucket bucket;

        /**
         * 创建一个新的定时任务
         * 
         * @param timer    关联的定时器实例
         * @param runnable 任务执行的具体逻辑
         * @param deadline 任务的截止时间（毫秒）
         */
        HashedWheelTimerTask(HashedWheelTimer timer, Runnable runnable, long deadline) {
            this.timer = timer;
            this.runnable = runnable;
            this.deadline = deadline;
        }

        /**
         * 取消定时任务
         * 将任务状态设置为已取消，并添加到定时器的已取消任务队列中
         * 已取消的任务不会被执行，并会在下一个时间轮周期中被清理
         */
        @Override
        public void cancel() {
            state = ST_CANCELLED;
            timer.cancelledTimeouts.add(this);
        }

        /**
         * 从时间轮中移除任务
         * 如果任务已分配到时间槽，则从槽中移除
         * 否则仅减少待处理任务计数
         */
        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        /**
         * 以原子方式比较并设置任务状态
         * 使用AtomicIntegerFieldUpdater确保线程安全
         * 
         * @param expected 预期的当前状态
         * @param state    要设置的新状态
         * @return 如果更新成功返回true，否则返回false
         */
        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        /**
         * 获取任务当前状态
         * 
         * @return 任务当前状态值
         */
        public int state() {
            return state;
        }

        /**
         * 检查任务是否已取消
         * 
         * @return 如果任务状态为ST_CANCELLED则返回true，否则返回false
         */
        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        /**
         * 检查任务是否已执行完毕
         * 
         * @return 如果任务状态为ST_EXPIRED则返回true，否则返回false
         */
        @Override
        public boolean isDone() {
            return state() == ST_EXPIRED;
        }

        /**
         * 执行定时任务
         * 首先尝试将任务状态从初始状态更新为已过期状态
         * 如果更新成功，则执行任务的runnable对象
         * 任务执行过程中的异常会被捕获并打印堆栈跟踪
         */
        public void execute() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }

            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        /**
         * 返回任务的字符串表示
         * 包含任务的截止时间、状态和关联的runnable对象信息
         * 
         * @return 任务的字符串表示
         */
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
     * 定时器分桶（时间槽）
     * 时间轮中的每个槽位对应一个分桶，用于存储在该时间点需要执行的任务
     * 内部使用双向链表结构组织多个定时任务
     */
    private static final class HashedWheelBucket {
        /**
         * 任务链表的头节点
         * 指向该时间槽中的第一个任务
         */
        private HashedWheelTimerTask head;
        
        /**
         * 任务链表的尾节点
         * 指向该时间槽中的最后一个任务
         */
        private HashedWheelTimerTask tail;

        /**
         * 向时间槽中添加一个定时任务
         * 任务会被添加到链表的尾部
         * 
         * @param timeout 要添加的定时任务
         */
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

        /**
         * 执行时间槽中所有到期的任务
         * 遍历链表中的所有任务，执行已到期或已取消的任务，并从链表中移除它们
         * 
         * @param deadline    当前时间轮指针对应的时间点
         * @param tickDuration 时间轮的时间精度
         */
        public void execute(long deadline, long tickDuration) {
            HashedWheelTimerTask timeout = head;
            while (timeout != null) {
                HashedWheelTimerTask next = timeout.next;
                if (timeout.deadline <= deadline || timeout.deadline < System.currentTimeMillis() + tickDuration) {
                    next = remove(timeout);
                    timeout.execute();
                } else if (timeout.isCancelled()) {
                    next = remove(timeout);
                }
                timeout = next;
            }
        }

        /**
         * 从时间槽中移除指定的定时任务
         * 维护双向链表的完整性，处理头尾节点的特殊情况
         * 
         * @param timeout 要移除的定时任务
         * @return 链表中的下一个任务，用于遍历过程中的指针移动
         */
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