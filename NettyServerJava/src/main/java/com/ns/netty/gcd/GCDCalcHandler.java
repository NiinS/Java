package com.ns.netty.gcd;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import com.ns.netty.commons.Pair;

public class GCDCalcHandler extends SimpleChannelInboundHandler<Pair<Integer, Integer>> 
{
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("GCD client handler is active.");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Pair<Integer, Integer> msg) throws Exception {
		int gcd = gcd(msg);
		ChannelFuture writeListener = ctx.writeAndFlush(gcd);
	}
	
	private int gcd(Pair<Integer, Integer> msg) {
		return 29;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		
	}
}
