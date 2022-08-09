package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultEventLoop;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;

public class KcpStringServer implements KcpListener {
    static DefaultEventLoop logicThread = new DefaultEventLoop();

    public static void main(String[] args) {

        KcpStringServer kcpStringServer = new KcpStringServer();
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true, 40, 2, true);
        channelConfig.setSndwnd(1024);
        channelConfig.setRcvwnd(1024);
        channelConfig.setMtu(1400);
        //channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()));
        //channelConfig.setFecAdapt(new FecAdapt(10,3));
        channelConfig.setAckNoDelay(false);
        //channelConfig.setCrc32Check(true);
        channelConfig.setTimeoutMillis(10000);

        //服务端传入参数
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(kcpStringServer, channelConfig, 10001);
    }


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来" + Thread.currentThread().getName() + ukcp.user().getRemoteAddress());
    }

    int i = 1;
    long start = System.currentTimeMillis();
    long end = System.currentTimeMillis();
    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
/*        byte[] bytes = new  byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(),bytes);
        System.out.println("收到消息: "+new String(bytes));
        kcp.write(buf);*/

        i++;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        System.out.println("收到消息: " + new String(bytes));
        kcp.write(buf);
        System.out.println("消息个数: " + i);
        end=System.currentTimeMillis();
        long gap=end-start;
        System.out.println("收发消息时间间隔为="+gap);
    }


    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp = new Snmp();
        System.out.println("连接断开了");
    }
}
