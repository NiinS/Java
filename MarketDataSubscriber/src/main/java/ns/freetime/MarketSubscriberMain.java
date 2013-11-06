package ns.freetime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MarketSubscriberMain 
{
    private static Logger log = LogManager.getLogger( MarketSubscriberMain.class);
    
    public static void main( String[] args )
    {
	//Startup all gateways
	
	//Ignite the Disruptor ring
	
	//Launch the UI
	
        log.info( "..." );
        
        long TIMES = 1000;
        long start = System.nanoTime();
        for(int i=1; i<TIMES; i++)
            concatUsingStringBuidder();
        long end = System.nanoTime();
        long stringBuilderTime = end - start;
        
        start = System.nanoTime();
        for(int i=1; i<TIMES; i++)
            concatUsingStringFormat();
        end = System.nanoTime();
        long stringFormatTime = end - start;
        
        boolean stBuilderTakesMore = stringBuilderTime > stringFormatTime;
        boolean stFormatTakesMore = stringBuilderTime < stringFormatTime;
        
        System.out.println(String.format( "StringBuilder time = %d, StringFormart time = %d", stringBuilderTime, stringFormatTime ));
        System.out.println("Stringbuilder takes more ? " + stBuilderTakesMore);
        System.out.println("StringFormatter takes more ? " + stFormatTakesMore);
    }

    private static void concatUsingStringFormat()
    {
	StringBuilder builder = new StringBuilder();
	builder.append( "This " ).append( "is year " ).append( 2013 );
    }

    private static void concatUsingStringBuidder()
    {
	String.format( "%s%s%d", "This ", "is year ", 2013);
    }
}
