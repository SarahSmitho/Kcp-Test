package kcp;

import io.netty.channel.socket.DatagramPacket;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by JinMiao
 * 2019/10/17.
 */
//服务器地址信道管理器
public class ServerAddressChannelManager implements IChannelManager {
    private Map<SocketAddress, Ukcp> ukcpMap = new ConcurrentHashMap<>();

    @Override
    public Ukcp get(DatagramPacket msg) {
        //A extends SocketAddress  参数返回的是地址
        return ukcpMap.get(msg.sender());
        //传入地址得到ukcp
    }

    @Override
    public void New(SocketAddress socketAddress, Ukcp ukcp,DatagramPacket msg) {
        //将元素加入映射
        ukcpMap.put(socketAddress, ukcp);
    }

    @Override
    public void del(Ukcp ukcp) {
        ukcpMap.remove(ukcp.user().getRemoteAddress());
    }

    @Override
    public Collection<Ukcp> getAll() {
        return this.ukcpMap.values();
    }
}
