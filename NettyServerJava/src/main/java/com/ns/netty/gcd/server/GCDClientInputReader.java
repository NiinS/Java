package com.ns.netty.gcd.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;

public class GCDClientInputReader extends ChannelInboundHandlerAdapter 
{
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Input parser recieved a channel activation event");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if( !( msg instanceof List) )
		{
			System.out.println("Bad input");
			return;
		}

		@SuppressWarnings("unchecked")
		List<Integer> in = (List<Integer>) msg;
		System.out.println("Client sent --> " + in);
		
		ctx.writeAndFlush( gcd(in) );
	}
	
	private Integer gcd(List<Integer> listOfIntegers) {
		
		int min = findAbsoluteMin(listOfIntegers);
		for(int k = min; k >= 1; k--)
		{
			if( isAllDivisible(listOfIntegers, k) )
				return k;
		}
		
		return -1; //to represent error -- this will never happen
	}

	private boolean isAllDivisible(List<Integer> listOfIntegers, int divisor) 
	{
		if(listOfIntegers == null || listOfIntegers.size() == 0)
			throw new IllegalArgumentException("List of integers is empty");

		for(int num : listOfIntegers)
		{
			if( (num % divisor) != 0 )
				return false;
		}
		
		return true;
	}

	private int findAbsoluteMin(List<Integer> listOfIntegers) 
	{
		if(listOfIntegers == null || listOfIntegers.size() == 0)
			throw new IllegalArgumentException("List of integers is empty");
		
		int least = Math.abs(listOfIntegers.get(0));
		for(int num : listOfIntegers)
		{
			int absValue = Math.abs(num);
			if( absValue < least )
				least = absValue;
		}
		
		return least;
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Channel inactive");
		super.channelInactive(ctx);
	}
}
