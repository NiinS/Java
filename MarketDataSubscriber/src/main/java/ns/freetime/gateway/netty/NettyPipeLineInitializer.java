package ns.freetime.gateway.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import ns.freetime.eventwheel.IMarketEventWheel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NettyPipeLineInitializer extends ChannelInitializer< Channel >
{

    private final Logger log = LogManager.getLogger( NettyPipeLineInitializer.class );
    private IMarketEventWheel eventWheel;
    
    public NettyPipeLineInitializer(IMarketEventWheel eventWheel)
    {
	this.eventWheel = eventWheel;
    }

    @Override
    protected void initChannel( Channel ch ) throws Exception
    {
	ChannelPipeline pipeline = ch.pipeline();
	pipeline.addLast( "frameDecoder", new LengthFieldBasedFrameDecoder( 1048576, 0, 4, 0, 4 ) );

	/*
	 * We do not want to create an immutable market event instance because
	 * our event wheel provides us mutable slots to fill with the raw
	 * events. So we can use those slots once and again.
	 * 
	 * Slots are basically protobuf "Builder" instances and can be merged
	 * from and build partially infinite times. So we don't need following
	 * decoder in our pipeline.
	 */

	// pipeline.addLast("protobufDecoder", new
	// ProtobufDecoder(MarketEvent.getDefaultInstance()));

	// This is our decoder which will merge the raw string with a re-usable
	// slot.
	pipeline.addLast( new RawMarketDataStringHandler(eventWheel) );

	log.info( "Added raw market event handler to pipeline." );
    }
}
