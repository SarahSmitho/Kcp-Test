package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;
import threadPool.disruptor.DisruptorExecutorPool;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * 测试吞吐量
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleServer implements KcpListener {
    public static void main(String[] args) {

        SpeedExampleServer speedExampleServer = new SpeedExampleServer();
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,30,2,true);
        //10000 比较稳定，在200MB到250之间，25000不稳定，波动很大有时候是在0，但是最大值也是200多
        //20000也不行，有时候回事70，波动很大;15000也不行
        channelConfig.setSndwnd(10000);
        channelConfig.setRcvwnd(10000);
        channelConfig.setMtu(1400);
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setAckNoDelay(true);
        channelConfig.setTimeoutMillis(5000);
        channelConfig.setUseConvChannel(true);
        channelConfig.setCrc32Check(false);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(speedExampleServer,channelConfig,20004);
    }

    long start = System.currentTimeMillis();


    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
    }

    long inBytes = 0;

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        inBytes+=buf.readableBytes();
        long now =System.currentTimeMillis();
        if(now-start>=1000){
            System.out.println("耗时 :" +(now-start) +" 接收数据: " +(Snmp.snmp.InBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+inBytes/1024.0/1024.0+" MB");
            System.out.println(Snmp.snmp.BytesReceived.doubleValue()/1024.0/1024.0);
            System.out.println(Snmp.snmp.toString());
            getDateAndTime();
            inBytes=0;
            Snmp.snmp = new Snmp();
            start=now;
        }
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp  = new Snmp();
        System.out.println("服务器断开");
        getDateAndTime();
    }

    public void getDateAndTime(){
        Calendar cal=Calendar.getInstance();
        int y=cal.get(Calendar.YEAR);
        int m=cal.get(Calendar.MONTH)+1;
        int d=cal.get(Calendar.DATE);
        int h=cal.get(Calendar.HOUR_OF_DAY);
        int mi=cal.get(Calendar.MINUTE);
        int s=cal.get(Calendar.SECOND);
        System.out.println("现在时刻是"+y+"年"+m+"月"+d+"日"+h+"时"+mi+"分"+s+"秒");
    }

}
