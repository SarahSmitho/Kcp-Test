package test;

import com.backblaze.erasure.FecAdapt;
import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import kcp.*;
import threadPool.disruptor.DisruptorExecutorPool;
import threadPool.disruptor.DisruptorSingleExecutor;

import java.net.InetSocketAddress;

/**
 * 测试单连接吞吐量
 * Created by JinMiao
 * 2019-06-27.
 */
public class KcpPingPongExampleClient implements KcpListener {

    //默认事件循环
    static DefaultEventLoop logicThread = new DefaultEventLoop();
    public static void main(String[] args) {

        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,40,2,true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()));
        //channelConfig.setFecAdapt(new FecAdapt(10,3));
        channelConfig.setAckNoDelay(false);
        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        //好像这些类都帮我们封装好了，客户端就创建一个KcpClient,服务端就创建一个KcpServer
        KcpClient kcpClient = new KcpClient();
        //把信道参数初始化
        kcpClient.init(channelConfig);

        //实现implements KcpListener接口对象
        KcpPingPongExampleClient kcpClientRttExample = new KcpPingPongExampleClient();
        //public Ukcp connect(InetSocketAddress remoteAddress, ChannelConfig channelConfig, KcpListener kcpListener)
        kcpClient.connect(new InetSocketAddress("192.168.3.216", 10001), channelConfig, kcpClientRttExample);
    }

    int i =0;

    //发送一百条消息
    @Override
    public void onConnected(Ukcp ukcp) {
        //发一百次信息停止？
        for (int i = 0; i < 100; i++) {
            //反正得到一个初始容量为1024的byteBuf
            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1024);
            //在当前writeIndex处写入一个int值，并将writeIndex增加4
            byteBuf.writeInt(i++);
            byte[] bytes = new byte[1020];
            //从当前writeIndex开始，传输来自于指定源(ByteBuf或者byte[])的数据。如果提供了srcIndex和length，则从srcIndex开始读取，
            // 并且处理长度为length的字节。当前writeIndex将会被增加所写入的字节数。
            //public abstract ByteBuf writeBytes(byte[] src);
            byteBuf.writeBytes(bytes);
            ukcp.write(byteBuf);
            byteBuf.release();
            System.out.println("i的值=" +i);
        }
        System.out.println("i的值=" +i);
    }
    int j =0;

    //处理接收到的消息
    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        //*返回共享整个区域的保留缓冲区。修改返回缓冲区或此缓冲区的内容会影响彼此的内容，同时它们保持单独的索引和标记。 ？？？
        //*此方法与{@code buf.slice（0，buf.capacity（））}相同。
        //*此方法不会修改这个缓冲区的｛@code readerIndex｝或｛@code writerIndex}
        ByteBuf newBuf = byteBuf.retainedDuplicate();
        //一个一直开着的循环事件线程
        logicThread.execute(() -> {
            try {
                ukcp.write(newBuf);
                newBuf.release();
                j++;
                if(j%100000==0){
                    System.out.println(Snmp.snmp.toString());
                    System.out.println("收到了 返回回去"+j);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        });

    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("连接断开了");
    }


}
