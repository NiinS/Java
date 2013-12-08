package ns.freetime.businessprocessor.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.table.DefaultTableModel;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public class QuoteTableModel extends DefaultTableModel
{
    private static final long serialVersionUID = - 190293979256286081L;

    private final static String[ ] COL_NAMES = { "Symbol", "Bid", "Ask", "LastUpdatedAt" };

    private final Map< String, Integer > symbolVsRowIndex;

    //
    // Time-stamp formatting
    //
    private static final String PATTERN = "YYYY-MM-dd 'at' HH:mm:ss.ZZZ";
    private final ThreadLocal< Date > threadlocalDate = new ThreadLocal<Date>();
    private final ThreadLocal< SimpleDateFormat > threadlocalDateFormatter = new ThreadLocal<SimpleDateFormat>();

    public QuoteTableModel()
    {
	super( COL_NAMES, 0 );
	symbolVsRowIndex = new ConcurrentHashMap< String, Integer >( 1024 );
	
    }

    public void marketQuoteReceived( MarketEvent quoteEvent )
    {
	if ( ! quoteEvent.hasSymbol() || !quoteEvent.hasBid() || !quoteEvent.hasAsk() )
	    return;

	String symbol = quoteEvent.getSymbol();
	
	double bid = quoteEvent.getBid();
	String bidAsStr = new BigDecimal(bid).setScale( 4, RoundingMode.HALF_EVEN ).toString();
	
	double ask = quoteEvent.getAsk();
	String askAsStr = new BigDecimal(ask).setScale( 4, RoundingMode.HALF_EVEN ).toString();
	
	long lastUpdateTime = quoteEvent.getTimeStamp();
	
	//Time formatting
	Date d = threadlocalDate.get();
	if(d == null)
	{
	    d = new Date();
	    threadlocalDate.set(d);
	}
	
	d.setTime( lastUpdateTime );
	
	SimpleDateFormat dateFormat = threadlocalDateFormatter.get();
	if(dateFormat == null)
	{
	    dateFormat = new SimpleDateFormat(PATTERN);
	    threadlocalDateFormatter.set(dateFormat);
	}
	
	String formattedTime = dateFormat.format( d );
	
	Integer rowIndex = symbolVsRowIndex.get( symbol );

	if ( rowIndex == null )
	{
	    Vector< Object > row = new Vector<>();
	    row.add( symbol );
	    row.add( bidAsStr );
	    row.add( askAsStr );
	    row.add( formattedTime );
	    
	    symbolVsRowIndex.put( symbol, getRowCount());
	    addRow( row );
	}
	else
	{
	    updateRow( rowIndex, symbol, bidAsStr, askAsStr, formattedTime);
	}
    }

    private void updateRow( Integer rowIndex, String symbol, String bid, String ask, String formattedTime )
    {
	setValueAt( symbol, rowIndex, 0);
	setValueAt( bid, rowIndex, 1);
	setValueAt( ask, rowIndex, 2);
	setValueAt( formattedTime, rowIndex, 3);
    }

}
