package com.ns.netty.gcd.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RemoteGCDCalculatorMain {
	
	private static int port;

	public static void main(String[] args) {
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(2);
		EventLoopGroup handlers = new NioEventLoopGroup(2);
		
		ServerBootstrap server = new ServerBootstrap();
		server.group(bossGroup, handlers)
			  .channel(NioServerSocketChannel.class)
			  .childHandler(new GCDServerInitializer());
		
		port = Integer.parseInt(args[0]);
		ChannelFuture channelFuture = server.bind(port);
		System.out.println("GCD server started listening on " + port);
		
		try 
		{
			channelFuture.channel().closeFuture().sync();
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally
		{
			bossGroup.shutdownGracefully();
			handlers.shutdownGracefully();
		}
		
	}
	
}
