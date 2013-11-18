package ns.freetime.gateway.netty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ns.freetime.gateway.IMarketGateway;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyProtobufGateway implements IMarketGateway
{
    private int port;
    private final Logger log = LogManager.getLogger( NettyProtobufGateway.class );
    
    public NettyProtobufGateway(int port)
    {
	this.port = port;
    }

    public void start()
    {
	EventLoopGroup bossGroup = new NioEventLoopGroup( 2 );
	EventLoopGroup handlers = new NioEventLoopGroup( 2 );

	ServerBootstrap server = new ServerBootstrap();
	server.group( bossGroup, handlers ).channel( NioServerSocketChannel.class ).childHandler( new NettyPipeLineInitializer() );

	ChannelFuture channelFuture = server.bind( port );
	log.info( String.format( "Netty based protobuf gateway started listening on port %d.", port ));

	try
	{
	    channelFuture.channel().closeFuture().sync();
	}
	catch ( InterruptedException e )
	{
	    e.printStackTrace();
	}
	finally
	{
	    bossGroup.shutdownGracefully();
	    handlers.shutdownGracefully();
	}

    }

    public void stop()
    {
	// TODO Auto-generated method stub

    }
}
