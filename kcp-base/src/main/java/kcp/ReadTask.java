package kcp;

import com.backblaze.erasure.fec.Snmp;
import internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import org.apache.log4j.Logger;
import threadPool.ITask;

import java.util.Queue;

/**
 * Created by JinMiao
 * 2018/9/11.
 */
//如果是服务端的话 只有     logger.error("3  把队列里的每一个元素输入到ukcp中  ukcp.input(byteBuf, current)");
//                      logger.error("4  ukcp.canRecv()");    7  走完ReadTask  最后退出的时候出现的

//如果是客户端的话 只有    1  ukcp为活跃状态    2  从ukcp获取ReadBuffer队列
//                      3  把队列里的每一个元素输入到ukcp中  ukcp.input   5  ukcp.notifyWriteEvent();   7  走完ReadTask
public class ReadTask implements ITask {
    private static Logger logger = Logger.getLogger(ReadTask.class);

    private final Ukcp ukcp;

    public ReadTask(Ukcp ukcp) {
        this.ukcp = ukcp;
    }


    @Override
    public void execute() {
        //就是一个List
        CodecOutputList<ByteBuf> bufList = null;
        Ukcp ukcp = this.ukcp;
        try {
            //查看连接状态，如果为Active不进入if语句，否则进入if语句
            if (!ukcp.isActive()) {
                return;
            }

            logger.error("1  ukcp为活跃状态");

            //获取当时时间
            long current = System.currentTimeMillis();
            //从ukcp获取ReadBuffer队列
            Queue<ByteBuf> recieveList = ukcp.getReadBuffer();

            logger.error("2  从ukcp获取ReadBuffer队列");

            int readCount =0;
            for (; ; ) {
                ByteBuf byteBuf = recieveList.poll();
                if (byteBuf == null) {
                    break;
                }
                readCount++;
                //把队列里的每一个元素输入到ukcp中
                ukcp.input(byteBuf, current);
                byteBuf.release();

                logger.error("3  把队列里的每一个元素输入到ukcp中  ukcp.input(byteBuf, current)");
            }
            if (readCount==0) {
                return;
            }

            if(ukcp.isControlReadBufferSize()){
                //这个逻辑不进去
                //控制readBufferSize   this.readBufferIncr.set(channelConfig.getReadBufferSize()/channelConfig.getMtu());
                ukcp.getReadBufferIncr().addAndGet(readCount);
                logger.error("0  ukcp.isControlReadBufferSize()");
            }

            long readBytes = 0;
            if (ukcp.isStream()) {
                //默认不是流模式
                int size =0;
                while (ukcp.canRecv()) {
                    if (bufList == null) {
                        bufList = CodecOutputList.newInstance();
                    }
                    ukcp.receive(bufList);
                    size= bufList.size();
                }

                for (int i = 0; i < size; i++) {
                    ByteBuf byteBuf = bufList.getUnsafe(i);
                    readBytes += byteBuf.readableBytes();
                    readBytebuf(byteBuf,current,ukcp);
                }
            } else {
                //这里给KcpListener通信用的，但是不知道为什么while(true)怎么出来
                while (ukcp.canRecv()) {
                    ByteBuf recvBuf = ukcp.mergeReceive();
                    readBytes += recvBuf.readableBytes();
                    readBytebuf(recvBuf,current,ukcp);
                    logger.error("4  ukcp.canRecv()");
                }
            }

            Snmp.snmp.BytesReceived.add(readBytes);
            //判断写事件
            //writeBuffer不为空且ukcp.canSend
            if (!ukcp.getWriteBuffer().isEmpty()&& ukcp.canSend(false)) {
                //把服务端 ukcp 的东西发回去？？？？
                ukcp.notifyWriteEvent();
                logger.error("5  ukcp.notifyWriteEvent();");
            }
        } catch (Throwable e) {
            //这个没加下面的也没进去
            logger.error("00  byteBuf成功来到  UKcp中的Queue<ByteBuf>");
            //ukcp.internalClose();
            e.printStackTrace();
           // logger.error("9",e);
        } finally {
            release();
            if (bufList != null) {
                //bufList不为空的话将会被重复使用
                bufList.recycle();
                logger.error("6  bufList不为空的话将会被重复使用");
            }
           logger.error("7  走完ReadTask");
        }
    }


    private void readBytebuf(ByteBuf buf,long current,Ukcp ukcp) {
        ukcp.setLastRecieveTime(current);
        try {
            ukcp.getKcpListener().handleReceive(buf, ukcp);
        } catch (Throwable throwable) {
            ukcp.getKcpListener().handleException(throwable, ukcp);
        }finally {
            buf.release();
        }
    }

    //TODO 这里看不懂
    public void release() {
        ukcp.getReadProcessing().set(false);
    }

}
