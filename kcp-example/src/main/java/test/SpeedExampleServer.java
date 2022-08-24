package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.ChannelConfig;
import kcp.KcpListener;
import kcp.KcpServer;
import kcp.Ukcp;
import threadPool.disruptor.DisruptorExecutorPool;

import java.util.Calendar;

/**
 * 测试吞吐量
 * Created by JinMiao
 * 2020/12/23.
 */
//这个就是服务端的类
public class SpeedExampleServer implements KcpListener {
    public static void main(String[] args) {

        //创建服务器对象
        SpeedExampleServer speedExampleServer = new SpeedExampleServer();

        //这里是信道配置，通过这些方法传进去
        ChannelConfig channelConfig = new ChannelConfig();
        //是否启动无延迟模式。无延迟模式rtomin将设置为0，拥塞控制不启动  retransmission TimeOut
        //nodelay启动无延迟模式
        channelConfig.nodelay(true,10,2,true);
        //设置服务器发送窗口
        channelConfig.setSndwnd(8000);
        //设置服务器接收窗口
        channelConfig.setRcvwnd(10000);
        //Maximum Transmission Unit  设置最大传输单元
        channelConfig.setMtu(1400);
        //这个是消息线程池我一般不管
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));

        //channelConfig.setFecDataShardCount(10);  这个是fec组件也能加快速度我还没学
        //channelConfig.setFecParityShardCount(3);
        //这个是设置ack马上发回去？我还不太理解
        channelConfig.setAckNoDelay(true);
        //超时时间 超过一段时间没收到消息断开连接，这个就是如果超过这个毫秒数没有接到消息服务器就会自动端来
        //这里，对的 单位毫秒 为了测试 作者设置5秒 现在已经找到一个原因，丢包没有传过来也会导致这个问题 是 老大想要都不断，要保持一天 是的
        //然后我只要调大这个超时时间，都不会断，老大要我找到里面的原理，不管它，反正他叫我做我就做
        //要就是客户端快点传过来，要就是超时时间变长，等那个丢包 那我之前直接把它这些关闭代码注释掉了
        channelConfig.setTimeoutMillis(5000);
        //这个是允许使用会话ID通信，conversatation id可以表示你们的通信，也可以不用，直接使用IP和端口
        channelConfig.setUseConvChannel(true);
        //这个还没学
        channelConfig.setCrc32Check(false);


        //这个是kcp的服务端对象
        KcpServer kcpServer = new KcpServer();
        //kcpServer.init的初始化方法
        kcpServer.init(speedExampleServer,channelConfig,20004);
    }

    long start = System.currentTimeMillis();
    long TestStart ;
    long TestEnd;

    @Override
    public void onConnected(Ukcp ukcp) {
        System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
        TestStart = System.currentTimeMillis();
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
            inBytes=0;
            getDateAndTime();
            Snmp.snmp = new Snmp();
            start=now;
        }
        //getDateAndTime();
/*        if(now-start>=5*60*1000){
            System.out.println("耗时 :" +(now-start)/1000/60+"分钟" +" 接收数据: " +(Snmp.snmp.InBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+inBytes/1024.0/1024.0+" MB");
            System.out.println(Snmp.snmp.BytesReceived.doubleValue()/1024.0/1024.0);
            System.out.println(Snmp.snmp.toString());
            getDateAndTime();
            inBytes=0;
            Snmp.snmp = new Snmp();
            start=now;
        }*/
    }

    //回调
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
        TestEnd = System.currentTimeMillis();
        System.out.println("传输活动持续时长="+(TestEnd-TestStart));
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
