package kcp;

import com.backblaze.erasure.fec.Fec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import threadPool.IMessageExecutor;
import threadPool.IMessageExecutorPool;

import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2018/9/20.
 */
//数据从这个类读进来的
public class ServerChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ServerChannelHandler.class);

    private IChannelManager channelManager;

    private ChannelConfig channelConfig;

    private IMessageExecutorPool iMessageExecutorPool;

    private KcpListener kcpListener;

    private HashedWheelTimer hashedWheelTimer;

    public ServerChannelHandler(IChannelManager channelManager, ChannelConfig channelConfig, IMessageExecutorPool iMessageExecutorPool, KcpListener kcpListener,HashedWheelTimer hashedWheelTimer) {
        this.channelManager = channelManager;
        this.channelConfig = channelConfig;
        this.iMessageExecutorPool = iMessageExecutorPool;
        this.kcpListener = kcpListener;
        this.hashedWheelTimer = hashedWheelTimer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("", cause);
        //SocketAddress socketAddress = ctx.channel().remoteAddress();
        //Ukcp ukcp = clientMap.get(socketAddress);
        //if(ukcp==null){
        //    logger.error("exceptionCaught ukcp is not exist address"+ctx.channel().remoteAddress(),cause);
        //    return;
        //}
        //ukcp.getKcpListener().handleException(cause,ukcp);
    }

    //netty会执行这个回调，
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        logger.error("channelRead执行");
        final ChannelConfig channelConfig = this.channelConfig;
        //接收DatagramPacket，这个就是数据包，数据传进来
        DatagramPacket msg = (DatagramPacket) object;
        Ukcp ukcp = channelManager.get(msg);
        //不管那么多得到的是msg里的数据
        ByteBuf byteBuf = msg.content();

        if (ukcp != null) {
            User user = ukcp.user();
            //每次收到消息重绑定地址
            //客户端的
            user.setRemoteAddress(msg.sender());
            //read函数是把byteBuf读到ukcp里的readBuffer里    this.readBuffer.offer(byteBuf);
            //把收到的byteBuf读进去，然后我们进去到这个函数
            ukcp.read(byteBuf);
            return;
        }

        //如果是新连接第一个包的sn必须为0
        int sn = getSn(byteBuf,channelConfig);
        if(sn!=0){
            msg.release();
            return;
        }
        IMessageExecutor iMessageExecutor = iMessageExecutorPool.getIMessageExecutor();
        KcpOutput kcpOutput = new KcpOutPutImp();
        Ukcp newUkcp = new Ukcp(kcpOutput, kcpListener, iMessageExecutor, channelConfig, channelManager);

        User user = new User(ctx.channel(), msg.sender(), msg.recipient());
        newUkcp.user(user);
        channelManager.New(msg.sender(), newUkcp, msg);

        iMessageExecutor.execute(() -> {
            try {
                newUkcp.getKcpListener().onConnected(newUkcp);
            } catch (Throwable throwable) {
                newUkcp.getKcpListener().handleException(throwable, newUkcp);
            }
        });

        newUkcp.read(byteBuf);
        //两个Ukcp目的是什么？？？？

        ScheduleTask scheduleTask = new ScheduleTask(iMessageExecutor, newUkcp,hashedWheelTimer);
        hashedWheelTimer.newTimeout(scheduleTask,newUkcp.getInterval(), TimeUnit.MILLISECONDS);
    }


    private int getSn(ByteBuf byteBuf,ChannelConfig channelConfig){
        int headerSize = 0;
        if(channelConfig.getFecAdapt()!=null){
            headerSize+= Fec.fecHeaderSizePlus2;
        }

        int sn = byteBuf.getIntLE(byteBuf.readerIndex()+Kcp.IKCP_SN_OFFSET+headerSize);
        return sn;
    }

}
