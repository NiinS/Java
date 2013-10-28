package com.ns.netty.gcd.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;

public class GCDNettyRequester extends ChannelInboundHandlerAdapter
{

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Channel is active , can send the request");
		
		for(int i=0;i<5; i++)
		{
			final ArrayList<Integer> list = new ArrayList<Integer>();
			list.add(24);
			list.add(8 + i);
			ctx.writeAndFlush(list);
		}
	}
	
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("Server respnded: GCD = " + msg);
	}
	
	
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}

}
