package ns.freetime.gateway.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ns.freetime.eventwheel.IMarketEventWheel;
import ns.freetime.gateway.IMarketGateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NettyProtobufGateway implements IMarketGateway
{
    private final int port;
    private final String id;
    private final IMarketEventWheel eventWheel;
    private final Logger log = LogManager.getLogger( NettyProtobufGateway.class );
    
    public NettyProtobufGateway(int port, IMarketEventWheel eventWheel)
    {
	this.port = port;
	this.eventWheel = eventWheel;
	id = "Netty-Protobuf-Gateway";
    }

    public void start()
    {
	EventLoopGroup bossGroup = new NioEventLoopGroup( 2 );
	EventLoopGroup handlers = new NioEventLoopGroup( 2 );

	ServerBootstrap server = new ServerBootstrap();
	server.group( bossGroup, handlers ).channel( NioServerSocketChannel.class ).childHandler( new NettyPipeLineInitializer(eventWheel) );

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
	//TODO do sth
    }

    public String getID()
    {
	return id;
    }
}
