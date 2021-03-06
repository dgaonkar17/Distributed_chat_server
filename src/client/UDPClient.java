package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SocketUtils;
import routing.MsgInterface.NetworkDiscoveryPacket;

public final class UDPClient {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    public static void main(String[] args) throws Exception {

        NetworkDiscoveryPacket.Builder builder = NetworkDiscoveryPacket.newBuilder();
        builder.setGroup("ClientGroup");
        builder.setSender(NetworkDiscoveryPacket.Sender.END_USER_CLIENT);
        builder.setNodeid("NODEID");

        NetworkDiscoveryPacket response = builder.build();



        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>(){
                        @Override
                        public void initChannel(DatagramChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new UDPClientHandler());
                        }
                    });


            Channel ch = b.bind(0).sync().channel();

            ByteBuf buf = Unpooled.copiedBuffer(response.toByteArray());


            ch.writeAndFlush(new DatagramPacket(buf,SocketUtils.socketAddress("127.0.0.1", PORT))).sync();

            // UDPClientHandler will close the DatagramChannel when a
            // response is received.  If the channel is not closed within 5 seconds,
            // print an error message and quit.
            if (!ch.closeFuture().await(5000)) {
                System.err.println("request timed out.");
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
