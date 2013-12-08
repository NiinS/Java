package ns.freetime.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import mockit.Mocked;
import mockit.Verifications;
import ns.freetime.businessprocessor.IMarketEventListener;
import ns.freetime.eventwheel.IMarketEventWheel;
import ns.freetime.eventwheel.disruptor.DisruptorWheelImpl;
import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Builder;
import ns.freetime.proto.MarketDataProto.MarketEvent.EventType;

import org.junit.Test;

public class EventWheelTests extends TestCase
{
    IMarketEventWheel eventsReactor;
    List< IMarketEventListener > businessListeners;

    @Mocked
    IMarketEventListener eventListener;

    private MarketEvent event;

    @Override
    protected void setUp() throws Exception
    {
	eventsReactor = new DisruptorWheelImpl( 1024, 2 );

	businessListeners = new ArrayList< IMarketEventListener >();
	businessListeners.add( eventListener );

	eventsReactor.registerMarketEventListener( businessListeners );
	eventsReactor.startRotating();

	Builder e = MarketEvent.newBuilder();
	e.setEventId( "Event" );
	e.setType( EventType.Quote );
	e.setTimeStamp( System.currentTimeMillis() );
	event = e.build();

    }

    @Test
    public void testEventsFired() throws Exception
    {
	byte[ ] bytes = event.toByteArray();
	eventsReactor.pushToWheel( bytes, 0, bytes.length );

	new Verifications()
	{
	    {
		for ( IMarketEventListener listner : businessListeners )
		    listner.onEvent( event );
	    }
	};
    }
}