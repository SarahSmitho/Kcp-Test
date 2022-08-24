package kcp;

import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.apache.log4j.Logger;


/**
 * Created by JinMiao
 * 2018/9/21.
 */
public class KcpOutPutImp implements KcpOutput {
    private static Logger logger = Logger.getLogger(KcpOutPutImp.class);
    //IKcp唯一实现 Kcp
    @Override
    public void out(ByteBuf data, IKcp kcp) {
        Snmp.snmp.OutPkts.increment();
        Snmp.snmp.OutBytes.add(data.writerIndex());

        //Kcp里Object user,转换再给User类？？？
        User user = (User) kcp.getUser();
        //创建DatagramPacket对象，传入data、RemoteAddress、LocalAddress
        //这里其实走完就把数据给Netty那边了，然后我学完
        DatagramPacket temp = new DatagramPacket(data,user.getRemoteAddress(), user.getLocalAddress());
        //public abstract class AbstractChannel extends DefaultAttributeMap implements Channel
        //把DatagramPacket给UDP发送
        user.getChannel().writeAndFlush(temp);
        logger.debug("13 kcp.output.out(data, kcp)");
    }
}
