package ns.freetime.businessprocessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public class UIMarketEventProcessor implements IMarketEventListener
{
    private final Logger log = LogManager.getLogger(UIMarketEventProcessor.class);
    private final ExecutorService businessEventProcessingPool = Executors.newFixedThreadPool( 2 );
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    public void onEvent( final MarketEvent event )
    {
	businessEventProcessingPool.execute( new Runnable()
	{
	    public void run()
	    {
		eventCounter.incrementAndGet();
		log.info( String.format("Received MarketData event #%d - %s", eventCounter.get(), event));
	    }
	} );
    }

}
