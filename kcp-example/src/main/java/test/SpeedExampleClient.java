package test;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import kcp.ChannelConfig;
import kcp.KcpClient;
import kcp.KcpListener;
import kcp.Ukcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.disruptor.DisruptorExecutorPool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Calendar;

/**
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleClient implements KcpListener {
    private static final Logger logger = LoggerFactory.getLogger(SpeedExampleClient.class);

    public SpeedExampleClient() {
    }

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();

        //是否启动无延迟模式。无延迟模式rtomin将设置为0，拥塞控制不启动
        channelConfig.nodelay(false,30,2,true);

        //send_windows size  设置发送窗口大小
        channelConfig.setSndwnd(800000000);

        //receive_windows size  设置接收窗口大小
        channelConfig.setRcvwnd(100000000);

        //Maximum Transmission Unit   最大传输单元，默认数据为1400，最小为50；
        channelConfig.setMtu(1400);

        //收到包立刻回传ack包
        channelConfig.setAckNoDelay(true);

        //conversation id  表示会话编号的整数，和TCP的 conv一样，通信双方需保证 conv相同，相互的数据包才能够被接受
        channelConfig.setConv(55);
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setCrc32Check(false);
        //channelConfig.setWriteBufferSize(channelConfig.getMtu()*80000000);
        channelConfig.setWriteBufferSize(channelConfig.getMtu()*80000000);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);

        //127.0.0.1   192.168.3.216    公司电脑 192.168.3.217
        SpeedExampleClient speedExampleClient = new SpeedExampleClient();
        kcpClient.connect(new InetSocketAddress("127.0.0.1",20004),channelConfig,speedExampleClient);

    }
    private static final int messageSize = 2048;
    private long start = System.currentTimeMillis();

    @Override
    public void onConnected(Ukcp ukcp) {
        //匿名内部类加Lambda表达式（把一些固定模式代码省略掉，适合熟悉的人）
        new Thread(() -> {
            //while(true) 和for（;;）是一样的
            for(;;){
                long now =System.currentTimeMillis();
                if(now-start>=1000){
                    System.out.println("耗时 :" +(now-start) +" 发送数据: " +(Snmp.snmp.OutBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+Snmp.snmp.BytesSent.doubleValue()/1024.0/1024.0+" MB");
                    System.out.println(Snmp.snmp.toString());
                    Snmp.snmp = new Snmp();
                    start=now;
                }
                //Cannot reserve 16777216 bytes of direct buffer memory (allocated: 4244643941, limit: 4250927104)
                //涉及到好多知识不管了
                //messageSize为2048
                ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(messageSize);
                //Byte数组长度
                //Byte[] bytes=new Byte[999999999];    这玩意阻塞耗资源
                //小米5G 本机跑的
                //调大这个数值能提高发送数据量，2048只有60MB左右，5000能达到70左右，9000报错，在byteBuf messageSize为2048情况下
                //8000最高85MB，8500报错，8250报错，8125最高86，稳定80
                //java.lang.OutOfMemoryError: Java heap space？？？系统分配JVM内存大小 1/64?

                //new byte[8125]  测试messageSize
                //messageSize 2048最高86MB 5000最高86没有变化

                //和这个参数有一定关系，窗口10000，messageSize 2048 最高170
                //10000报错
               /* File file = new File("E:\\视频","5.mp4");
                System.out.println("构建file对象成功");
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream= new FileInputStream(file);
                    System.out.println("构建输入流对象成功");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                byte[] imageData = new byte[0];
                try {
                    imageData = new byte[fileInputStream.available()];
                    System.out.println("生成byte数组成功");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                int length= 0;
                try {
                    length = fileInputStream.available();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    int readSize = 0;
                    while (readSize < length){
                        int size = fileInputStream.read(imageData, readSize, length - readSize);
                        System.out.println("size大小="+size);
                        byteBuf.writeBytes(imageData,readSize,size);
                        readSize = readSize + size;
                        System.out.println("readSize大小="+readSize);
                    };
                    System.out.println("从输入流对象把数据读入imageData成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                byteBuf.writeBytes(new byte[1000]);
                logger.debug("0  开始往ukcp写入数据");
                if(!ukcp.write(byteBuf)){
                    try {
                        Thread.sleep(1000);
                        //System.out.println("ukcp.write(byteBuf)="+ukcp.write(byteBuf));
                       //System.out.println("出问题了");
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }
                byteBuf.release();
            }
        }).start();
    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp)
    {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        //客户端只会不断发数据，没有断开这一说
        System.out.println("客户端断开");
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
