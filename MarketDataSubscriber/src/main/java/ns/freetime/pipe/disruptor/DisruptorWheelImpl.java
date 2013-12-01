package ns.freetime.pipe.disruptor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ns.freetime.businessprocessor.IMarketEventListener;
import ns.freetime.pipe.IMarketEventWheel;
import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Builder;
import ns.freetime.proto.MarketEventBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorWheelImpl implements IMarketEventWheel
{

    private final Disruptor< MarketEvent.Builder > disruptor;
    private final Logger log = LogManager.getLogger( DisruptorWheelImpl.class );
    private final List< IMarketEventListener > businessEventListeners;

    @SuppressWarnings( "unchecked" )
    public DisruptorWheelImpl( int maxSlots, int numEventProcessors )
    {
	if ( ( maxSlots & ( maxSlots - 1 ) ) != 0 )
	    throw new IllegalArgumentException( "Number of slots in the event wheel should be a power of 2." );

	if ( numEventProcessors <= 0 || numEventProcessors > 20 )
	    numEventProcessors = 2;

	this.businessEventListeners = new CopyOnWriteArrayList< IMarketEventListener >();

	ExecutorService pool = Executors.newFixedThreadPool( numEventProcessors );

	disruptor = new Disruptor< MarketEvent.Builder >( new MarketEventBuilderFactory(), maxSlots, pool, ProducerType.MULTI, new YieldingWaitStrategy() );

	disruptor.handleEventsWith( new EventHandler< MarketEvent.Builder >()
	{
	    public void onEvent( MarketEvent.Builder eventBuilder, long sequence, boolean endOfBatch ) throws Exception
	    {
		MarketEvent businessEvent = eventBuilder.buildPartial();
		for ( IMarketEventListener listener : businessEventListeners )
		{
		    try
		    {
			listener.onEvent( businessEvent );
		    }
		    catch ( Exception ex )
		    {
			log.warn( "Exception during business event handling.", ex );
		    }
		}
	    }
	} );
    }

    public void startRotating()
    {
	if ( disruptor != null )
	    disruptor.start();
    }

    public void stopRotating()
    {
	if ( disruptor != null )
	    disruptor.shutdown();
    }

    public void pushToWheel( byte[ ] array, int offset, int length )
    {
	if ( array == null || offset < 0 || length < 0 || offset > ( length - 1 ) )
	    throw new IllegalArgumentException( "Raw data passed was invalid." );

	RingBuffer< Builder > ringBuffer = disruptor.getRingBuffer();
	long claimedSlot = ringBuffer.next();

	try
	{
	    MarketEvent.Builder eventBuilderToFill = ringBuffer.get( claimedSlot );
	    eventBuilderToFill.clear();
	    eventBuilderToFill.mergeFrom( array, offset, length );
	}
	catch ( InvalidProtocolBufferException e )
	{
	    log.error( "Could not build proto event from passed bytes array", e );
	}
	finally
	{
	    ringBuffer.publish( claimedSlot );
	}
    }

    public void registerMarketEventListener( List< IMarketEventListener > eventListeners )
    {
	if ( eventListeners != null )
	{
	    businessEventListeners.addAll( eventListeners );
	}
    }
}
