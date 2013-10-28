package com.ns.netty.gcd.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class GCDNettyClientChannelInitializer extends ChannelInitializer<Channel>  
{
	@Override
	protected void initChannel(Channel ch) throws Exception {
		
		ch.pipeline().addLast( new ObjectEncoder() );
		ch.pipeline().addLast( new ObjectDecoder(ClassResolvers.cacheDisabled(null)) );
		ch.pipeline().addLast(new GCDNettyRequester());
		System.out.println("Netty client requester added to pipeline.");
	}
}
