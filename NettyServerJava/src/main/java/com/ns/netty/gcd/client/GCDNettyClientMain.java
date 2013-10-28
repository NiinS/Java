package com.ns.netty.gcd.client;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class GCDNettyClientMain {
	
	public static void main(String[] args) {
		
		NioEventLoopGroup workerGrp = new NioEventLoopGroup();
		
		Bootstrap clientBootstrap = new Bootstrap();
		clientBootstrap.group(workerGrp);
		clientBootstrap.channel(NioSocketChannel.class);
		clientBootstrap.handler(new GCDNettyClientChannelInitializer());
		
		
		try 
		{
			
			final int port = Integer.parseInt(args[0]);
			ChannelFuture connect = clientBootstrap.connect(new InetSocketAddress(port)).sync();
			
			connect.addListener(new GenericFutureListener<Future<? super Void>>() {
				public void operationComplete(Future<? super Void> future) throws Exception 
				{
					System.out.println("Connected to server at port " + port);
				};
			});
			
			connect.channel().closeFuture().sync();
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		finally
		{
			workerGrp.shutdownGracefully();
		}
		
	}

}
