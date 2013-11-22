package ns.freetime.gateway.netty;

import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Builder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RawMarketDataStringHandler extends SimpleChannelInboundHandler< String >
{

    private final Logger log = LogManager.getLogger(RawMarketDataStringHandler.class); 
    
    @Override
    protected void channelRead0( ChannelHandlerContext ctx, String msg ) throws Exception
    {
	log.info( "Received raw MarketData " );
	Builder mData = MarketEvent.newBuilder().mergeFrom( msg.getBytes());
    }

}
