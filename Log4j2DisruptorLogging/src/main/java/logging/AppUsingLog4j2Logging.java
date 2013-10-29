package logging;

import org.apache.logging.log4j.*;

public class AppUsingLog4j2Logging 
{
    public static void main( String[] args )
    {
        logger.info("Logging with Apache log4j2" );
    }
    
    private static final Logger logger = LogManager.getLogger(AppUsingLog4j2Logging.class.getName());
}
