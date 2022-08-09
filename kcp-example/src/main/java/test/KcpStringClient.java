package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.DefaultEventLoop;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;

import java.net.InetSocketAddress;

public class KcpStringClient implements KcpListener {
    static DefaultEventLoop logicThread = new DefaultEventLoop();

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true, 40, 2, true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()));
        //channelConfig.setFecAdapt(new FecAdapt(10,3));
        channelConfig.setAckNoDelay(false);
        //channelConfig.setCrc32Check(true);
        //channelConfig.setTimeoutMillis(10000);

        //kcpClient用来实现channelConfig的，把接口KcpStringClient implements KcpListener作为参数传入kcpClient中
        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);

        KcpStringClient kcpStringClient = new KcpStringClient();

        kcpClient.connect(new InetSocketAddress("127.0.0.1", 10001), channelConfig, kcpStringClient);
    }

    long start = System.currentTimeMillis();

    @Override
    public void onConnected(Ukcp ukcp) {
        long now = start;
        int times = 1;
        System.out.println("1秒钟发送的文本信息次数=" + times);
        while (now - start <= 1000) {
            String msg = "hello!!!!!11111111111111111111111111";
            byte[] bytes = msg.getBytes();
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.ioBuffer(bytes.length);
            byteBuf.writeBytes(bytes);
            ukcp.write(byteBuf);
            byteBuf.release();
            now = System.currentTimeMillis();
            times++;
            System.out.println("1秒钟发送的文本信息次数=" + times);
        }
        System.out.println("1秒钟发送的文本信息次数=" + times);
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
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
