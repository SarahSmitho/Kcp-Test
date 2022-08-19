package kcp;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.apache.log4j.Logger;
import threadPool.IMessageExecutor;
import threadPool.ITask;

import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2018/10/24.
 */
//Schedule 日程
public class ScheduleTask implements ITask, Runnable, TimerTask {
    private static Logger logger = Logger.getLogger(ScheduleTask.class);

    //消息处理器
    // void execute(ITask iTask); * 执行任务  注意: 如果线程等于当前线程 则直接执行  如果非当前线程放进队列
    private final IMessageExecutor messageExecutor;

    private final Ukcp ukcp;

    private final HashedWheelTimer hashedWheelTimer;

    public ScheduleTask(IMessageExecutor messageExecutor, Ukcp ukcp, HashedWheelTimer hashedWheelTimer) {
        this.messageExecutor = messageExecutor;
        this.ukcp = ukcp;
        this.hashedWheelTimer = hashedWheelTimer;
    }

    //flush策略
    //1,在send调用后检查缓冲区如果可以发送直接调用update得到时间并存在ukcp内
    //2，定时任务到了检查ukcp的时间和自己的定时 如果可以发送则直接发送  时间延后则重新定时
    //定时任务发送成功后检测缓冲区  是否触发发送时间
    //3，读时间触发后检测检测缓冲区触发写事件
    //问题: 精准大量的flush触发会导致ack重复发送   流量增大？  不会的 ack只会发送一次
    @Override
    public void execute() {
        logger.error("ScheduleTask执行");
        try {
            final Ukcp ukcp = this.ukcp;
            long now = System.currentTimeMillis();
            //判断连接是否关闭
            if (ukcp.getTimeoutMillis() != 0 && now - ukcp.getTimeoutMillis() > ukcp.getLastRecieveTime()) {
                System.out.println(" 在ScheduleTask中断开 ");
                ukcp.internalClose();
            }
            if (!ukcp.isActive()) {
                return;
            }
            long timeLeft = ukcp.getTsUpdate() - now;
            //判断执行时间是否到了
            if (timeLeft > 0) {
                hashedWheelTimer.newTimeout(this,timeLeft, TimeUnit.MILLISECONDS);
                return;
            }
            long next = ukcp.flush(now);
            hashedWheelTimer.newTimeout(this,next, TimeUnit.MILLISECONDS);
            //检测写缓冲区 如果能写则触发写事件
            if (!ukcp.getWriteBuffer().isEmpty() && ukcp.canSend(false))
            {
                ukcp.notifyWriteEvent();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        // void execute(ITask iTask); * 执行任务  注意: 如果线程等于当前线程 则直接执行  如果非当前线程放进队列
        this.messageExecutor.execute(this);
    }

    @Override
    public void run(Timeout timeout) {
        run();
    }
}
