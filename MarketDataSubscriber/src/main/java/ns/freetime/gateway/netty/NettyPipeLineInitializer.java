package ns.freetime.gateway.netty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class NettyPipeLineInitializer extends  ChannelInitializer< Channel >
{

    private final Logger log = LogManager.getLogger(NettyPipeLineInitializer.class); 
	    
    @Override
    protected void initChannel( Channel ch ) throws Exception
    {
	ChannelPipeline pipeline = ch.pipeline();
	pipeline.addLast( new RawMarketDataStringHandler() );
	log.info("Added raw string handler to pipeline.");
    }
}
