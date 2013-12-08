package ns.freetime;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;

import ns.freetime.businessprocessor.IMarketEventListener;
import ns.freetime.businessprocessor.ui.MainUICanvas;
import ns.freetime.eventwheel.IMarketEventWheel;
import ns.freetime.gateway.IMarketGateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class MarketSubscriberMain
{
    private static final Logger log = LogManager.getLogger( MarketSubscriberMain.class );
    private final List< IMarketGateway > gateways;
    private final IMarketEventWheel eventWheel;
    private final List< IMarketEventListener > businessEventListeners;
    private final MainUICanvas userInterface;

    public MarketSubscriberMain(MainUICanvas userInterface, List< IMarketGateway > gateways, IMarketEventWheel eventWheel, List<IMarketEventListener> businessEventListeners )
    {
	this.userInterface = userInterface;
	this.gateways = gateways;
	this.eventWheel = eventWheel;
	this.businessEventListeners = businessEventListeners;
    }

    public static void main( String[ ] args )
    {
	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext( "context.xml" );
	MarketSubscriberMain subscriber = context.getBean( MarketSubscriberMain.class );
	subscriber.start();
    }

    private void start()
    {
	// Launch the UI
	launchMarketDataViewer(businessEventListeners);

	// Ignite the wheel
	eventWheel.registerMarketEventListener( businessEventListeners );
	eventWheel.startRotating();
	log.info( "Event wheel started" );

	// Startup all gateways
	startGatewaysAsync();

    }

    private void launchMarketDataViewer(List< IMarketEventListener > businessEventListeners2)
    {
        final JFrame frame = new JFrame("MarketData ReadOnly");
        frame.getContentPane().add(userInterface.getContent(),BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 200);
        frame.setVisible(true);
    }

    private void startGatewaysAsync()
    {
	// Not expecting too many configured gateways - can use a cached pool
	ExecutorService pool = Executors.newFixedThreadPool( 2 );

	ListeningExecutorService listeningDecorator = MoreExecutors.listeningDecorator( pool );

	final AtomicInteger numNotRunningGateways = new AtomicInteger( 0 );

	// This async function is invoked every time a gateway stops running.
	// This function checks if all
	// gateways have stopped and if yes, it stops the system.
	AsyncFunction< IMarketGateway, IMarketGateway > ALL_GATEWAYS_STOPPED = new AsyncFunction< IMarketGateway, IMarketGateway >()
	{
	    public ListenableFuture< IMarketGateway > apply( IMarketGateway input ) throws Exception
	    {
		if ( numNotRunningGateways.incrementAndGet() == gateways.size() )
		{
		    log.info( "MarketDataSubscriber is exiting because no gateway is running." );
		    System.exit( 0 );
		}

		return Futures.immediateFuture( input );
	    }
	};

	for ( final IMarketGateway gw : gateways )
	{
	    ListenableFuture< IMarketGateway > future = listeningDecorator.submit( new Callable< IMarketGateway >()
	    {
		public IMarketGateway call()
		{
		    try
		    {
			gw.start();
		    }
		    catch ( Exception ex )
		    {
			log.warn( String.format( "Event gateway %s could not start", gw.getID() ), ex );
		    }

		    return gw;
		}
	    } );

	    // Every time a gateway stops running, apply this function
	    Futures.transform( future, ALL_GATEWAYS_STOPPED );
	}
    }

}
