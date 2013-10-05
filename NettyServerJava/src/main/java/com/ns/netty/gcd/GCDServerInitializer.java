package com.ns.netty.gcd;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class GCDServerInitializer extends ChannelInitializer<Channel>  
{
	@Override
	protected void initChannel(Channel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
//        pipeline.addLast("decoder", new BigIntegerDecoder());
//        pipeline.addLast("encoder", new NumberEncoder());

		//pipeline.addLast(new GCDClientInputReader());
		//pipeline.addLast(new GCDCalcHandler());
		
		pipeline.addLast( new ObjectEncoder() );
		pipeline.addLast( new ObjectDecoder(ClassResolvers.cacheDisabled(null)) );
		pipeline.addLast(new GCDClientInputReader());
//		pipeline.addLast(new GCDCalcHandler());
	}
}
