package sample;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.util.Random;

import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Builder;
import ns.freetime.proto.MarketDataProto.MarketEvent.EventType;
import ns.freetime.proto.MarketDataProto.MarketEvent.Side;
import ns.freetime.proto.MarketDataProto.MarketEvent.Status;
import ns.freetime.proto.MarketDataProto.MarketEvent.TradeType;

public class SampleMarketDataSource
{
    public static void main( String[ ] args )
    {

	NioEventLoopGroup workerGrp = new NioEventLoopGroup();

	Bootstrap clientBootstrap = new Bootstrap();
	clientBootstrap.group( workerGrp );
	clientBootstrap.channel( NioSocketChannel.class );
	clientBootstrap.handler( new SampleMarketDataSourceChannelInitializer() );

	try
	{

	    final int port = Integer.parseInt( args [0] );
	    ChannelFuture connect = clientBootstrap.connect( new InetSocketAddress( port ) ).sync();

	    connect.addListener( new GenericFutureListener< Future< ? super Void >>()
	    {
		public void operationComplete( Future< ? super Void > future ) throws Exception
		{
		    System.out.println( "Connected to server at port " + port );
		};
	    } );

	    connect.channel().closeFuture().sync();
	}
	catch ( InterruptedException e )
	{
	    e.printStackTrace();
	}
	finally
	{
	    workerGrp.shutdownGracefully();
	}

    }

    private static class SampleMarketDataSourceChannelInitializer extends ChannelInitializer< Channel >
    {
	@Override
	protected void initChannel( Channel ch ) throws Exception
	{
	    // To send as protobuf
	    ch.pipeline().addLast( "frameEncoder", new LengthFieldPrepender( 4 ) );
	    ch.pipeline().addLast( "protobufEncoder", new ProtobufEncoder() );
	    ch.pipeline().addLast( new SampleMarketDataSourcePublisher() );

	    // To send as string
	    // ch.pipeline().addLast( new ObjectEncoder() );
	    // ch.pipeline().addLast( new ObjectDecoder(
	    // ClassResolvers.cacheDisabled( null ) ) );
	    // ch.pipeline().addLast( new SampleMarketDataSourcePublisher() );

	    System.out.println( "Netty publisher added to pipeline." );
	}
    }

    public static class SampleMarketDataSourcePublisher extends ChannelInboundHandlerAdapter
    {

	private final Random randomEventSelector;
	private final Random randomSymbolSelector;

	private static final String[ ] SYMBOL_REPO = { "INR/USD", "INR/GBP", "INR/RUB", "INR/SGD", "INR/AUD", "INR/CHF", "INR/NZD" };

	public SampleMarketDataSourcePublisher()
	{
	    randomSymbolSelector = new Random();
	    randomEventSelector = new Random();
	}

	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
	    System.out.println( "Channel is active , can send market data" );

	    for ( int i = 1 ; i <= 50 ; i++ )
	    {
		Builder event = MarketEvent.newBuilder();

		event.setEventId( "Event_" + i );

		event.setTimeStamp( System.currentTimeMillis() );

		setAsQuoteOrTrade( event );

		MarketEvent marketEvent = event.build();
		ctx.writeAndFlush( marketEvent );
		System.out.println( "Sent " + marketEvent.toString() );
		Thread.sleep( 500 );
	    }
	}

	private void setAsQuoteOrTrade( Builder event )
	{
	    boolean shouldCreateQuote = randomEventSelector.nextBoolean();
	    
	    int randomSymbolIndex = (int)((double)SYMBOL_REPO.length * Math.random());
	    event.setSymbol( SYMBOL_REPO [randomSymbolIndex] );
	    
	    double randomBasePrice = randomSymbolSelector.nextDouble();

	    if ( shouldCreateQuote )
	    {
		event.setType( EventType.Quote );
		
		event.setAsk( randomBasePrice + 3.55 );
		event.setBid( randomBasePrice + 3.51 );
	    }
	    else
	    {
		event.setType( EventType.Trade );
		
		TradeType[ ] tradeTypes = TradeType.values();
		int typeIndex = (int)((double)tradeTypes.length * Math.random());
		event.setTradeType( tradeTypes[typeIndex]);
		
		Side[ ] sides = Side.values();
		int sideIndex = (int)((double)sides.length * Math.random());
		event.setSide( sides[sideIndex] );
		
		event.setBid( randomBasePrice + 2.25 );
		event.setAsk( randomBasePrice + 3.25 );
		
		event.setFirm( "Client_A_" + SYMBOL_REPO[randomSymbolIndex] );
		event.setCounterFirm( "CounterClient_A_" + SYMBOL_REPO[randomSymbolIndex]  );

		Status[ ] statuses = Status.values();
		int statusIndex = (int)((double)statuses.length * Math.random());
		Status status = statuses[statusIndex]; 
		event.setStatus(status );
		
		event.setQuantity( 100000000 );
		if(status == Status.Done)
		    event.setExecutedQty( event.getQuantity() );
	    }
	}

	@Override
	public void channelRead( ChannelHandlerContext ctx, Object msg ) throws Exception
	{
	    System.out.println( "Server respnded: " + msg );
	}

	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
	{
	    cause.printStackTrace();
	}

    }

}
