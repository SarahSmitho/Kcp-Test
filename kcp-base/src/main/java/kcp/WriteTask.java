package kcp;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import org.apache.log4j.Logger;
import threadPool.ITask;

import java.io.IOException;
import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
//TODO　其实追到execute() 这就可以往这个方法追了看谁调用他  左键 CTRL WriteTask
public class WriteTask implements ITask {
    private static Logger logger = Logger.getLogger(WriteTask.class);

    private final Ukcp ukcp;

    public WriteTask(Ukcp ukcp) {
        this.ukcp = ukcp;
    }

    @Override
    public void execute() {
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态
            if(!ukcp.isActive()){
                return;
            }
            //之前是追数据，还没有执行数据 我们只是追数据往哪走 其实也一样了，只是有一个方法
            //从发送缓冲区到kcp缓冲区   ????
            //从Queue拿出来
            Queue<ByteBuf> queue = ukcp.getWriteBuffer();
            logger.debug("2  从ukcp中的queue  Queue<ByteBuf> queue = ukcp.getWriteBuffer()");
            logger.error("2  从ukcp中的queue  Queue<ByteBuf> queue = ukcp.getWriteBuffer()");
            int writeCount =0;
            long writeBytes = 0;
            //这句话更加重要
            while(ukcp.canSend(false)){
                //得到队列头部的元素
                ByteBuf byteBuf = queue.poll();
                logger.debug("3  得到队列queue中的头部元素   ByteBuf byteBuf = queue.poll()");
                logger.error("3  得到队列queue中的头部元素   ByteBuf byteBuf = queue.poll()");
                if(byteBuf==null){
                    break;
                }
                writeCount++;
                try {
                    writeBytes +=byteBuf.readableBytes();
                    //这里发送逻辑
                    ukcp.send(byteBuf);
                    logger.debug("7  ukcp的发送函数结束  ukcp.send(byteBuf)");
                    logger.error("7  ukcp的发送函数结束  ukcp.send(byteBuf)");
                    byteBuf.release();
                } catch (IOException e) {
                    ukcp.getKcpListener().handleException(e, ukcp);
                    return;
                }
            }
            Snmp.snmp.BytesSent.add(writeBytes);
            if(ukcp.isControlWriteBufferSize()){
                ukcp.getWriteBufferIncr().addAndGet(writeCount);
            }
            //如果有发送 则检测时间    这句话给我们提示  ukcp.canSend不能也flush一下
            if(!ukcp.canSend(false)||(ukcp.checkFlush()&& ukcp.isFastFlush())){
                long now =System.currentTimeMillis();
                //TODO 从这里追  看一下怎么能想到从这里追呢
                long next = ukcp.flush(now);
                ukcp.setTsUpdate(now+next);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            release();
        }
    }


    public void release(){
        ukcp.getWriteProcessing().set(false);
    }
}
