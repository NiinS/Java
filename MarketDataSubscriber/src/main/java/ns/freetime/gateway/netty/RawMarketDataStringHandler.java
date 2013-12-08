package ns.freetime.gateway.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import ns.freetime.eventwheel.IMarketEventWheel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RawMarketDataStringHandler extends MessageToMessageDecoder< ByteBuf >
{

    private final Logger log = LogManager.getLogger( RawMarketDataStringHandler.class );
    private IMarketEventWheel wheel;

    public RawMarketDataStringHandler(IMarketEventWheel wheel)
    {
	this.wheel = wheel;
    }
    
    @Override
    protected void decode( ChannelHandlerContext ctx, ByteBuf msg, List< Object > out ) throws Exception
    {
	final byte[ ] array;
	final int offset;
	final int length = msg.readableBytes();
	if ( msg.hasArray() )
	{
	    array = msg.array();
	    offset = msg.arrayOffset() + msg.readerIndex();
	}
	else
	{
	    array = new byte [ length ];
	    msg.getBytes( msg.readerIndex(), array, 0, length );
	    offset = 0;
	}

	wheel.pushToWheel( array, offset, length );
	//log.info( "Received raw MarketData " + MarketEvent.getDefaultInstance().newBuilderForType().mergeFrom( array, offset, length ).build());
	
    }

}
