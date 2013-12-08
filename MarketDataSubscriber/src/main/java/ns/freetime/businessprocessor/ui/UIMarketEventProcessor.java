package ns.freetime.businessprocessor.ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ns.freetime.businessprocessor.IMarketEventListener;
import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.EventType;

public class UIMarketEventProcessor implements IMarketEventListener
{
    private final Logger log = LogManager.getLogger(UIMarketEventProcessor.class);
    private final ExecutorService businessEventProcessingPool = Executors.newFixedThreadPool( 2 );
    private final AtomicLong eventCounter = new AtomicLong(0);
    private MainUICanvas userInterface;
    
    public UIMarketEventProcessor(MainUICanvas userInterface)
    {
	this.userInterface = userInterface;
    }
    
    public void onEvent( final MarketEvent event )
    {
	businessEventProcessingPool.execute( new Runnable()
	{
	    public void run()
	    {
		eventCounter.incrementAndGet();
		
		if(!event.hasType())
		    return;
		
		EventType eventType = event.getType();
		
		if(eventType == MarketEvent.EventType.Quote)
		    userInterface.marketQuoteReceived( event );
		else
		    userInterface.tradeEventReceived( event );
		
		log.info( String.format("Received MarketData event #%d - %s", eventCounter.get(), event));
	    }
	} );
    }

}
