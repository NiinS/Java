package ns.freetime;

import java.util.List;

import ns.freetime.gateway.IMarketGateway;
import ns.freetime.pipe.IMarketEventWheel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MarketSubscriberMain
{
    private static final Logger log = LogManager.getLogger( MarketSubscriberMain.class );
    private final List< IMarketGateway > gateways;
    private final IMarketEventWheel eventWheel;

    public MarketSubscriberMain( List< IMarketGateway > gateways, IMarketEventWheel eventWheel )
    {
	this.gateways = gateways;
	this.eventWheel = eventWheel;
    }

    public static void main( String[ ] args )
    {
	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext( "config.xml" );
	MarketSubscriberMain subscriber = context.getBean( MarketSubscriberMain.class );
	subscriber.start();
    }

    private void start()
    {
	// Ignite the wheel
	eventWheel.startRotating();
	log.info( "Event wheel started" );

	// Startup all gateways
	for ( IMarketGateway gw : gateways )
	{
	    try
	    {
		gw.start();
	    }
	    catch ( Exception ex )
	    {
		log.warn( String.format( "Event gateway %s could not start", gw.getID() ), ex );
	    }
	}

	// Launch the UI

	log.info( "..." );
    }

}
