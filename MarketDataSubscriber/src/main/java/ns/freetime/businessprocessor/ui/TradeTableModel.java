package ns.freetime.businessprocessor.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Side;
import ns.freetime.proto.MarketDataProto.MarketEvent.Status;
import ns.freetime.proto.MarketDataProto.MarketEvent.TradeType;

public class TradeTableModel extends DefaultTableModel
{
    private static final long serialVersionUID = 8641503060090852428L;

    private static String[ ] COL_NAMES = { "TradEventID" ,"Symbol", "Type", "Side", "OrderQty", "ExecutedQty", "ExecutionPrice", "Party", "CounterParty", "Status", "DateTime" };

    //
    // Time-stamp formatting
    //
    private static final String PATTERN = "YYYY-MM-dd 'at' HH:mm:ss.ZZZ";
    private final ThreadLocal< Date > threadlocalDate = new ThreadLocal< Date >();
    private final ThreadLocal< SimpleDateFormat > threadlocalDateFormatter = new ThreadLocal< SimpleDateFormat >();

    public TradeTableModel()
    {
	super( COL_NAMES, 0 );
    }

    public void tradeEventReceived( MarketEvent tradeEvent )
    {
	if ( ! tradeEvent.hasSymbol() || ! tradeEvent.hasTradeType() || ! tradeEvent.hasSide() || ! tradeEvent.hasQuantity() || ! tradeEvent.hasFirm() || ! tradeEvent.hasCounterFirm() || ! tradeEvent.hasStatus() )
	    return;

	//
	// Symbol
	//
	String symbol = tradeEvent.getSymbol();

	//
	//Side and Price
	//
	double executedAtPrice = 0;
	Side side = tradeEvent.getSide();
	if ( side == MarketEvent.Side.Buy )
	    executedAtPrice = tradeEvent.getBid();
	else
	    executedAtPrice = tradeEvent.getAsk();
	String priceAsStr = new BigDecimal( executedAtPrice ).setScale( 4, RoundingMode.HALF_EVEN ).toString();

	//
	// Time of trade
	//
	long lastUpdateTime = tradeEvent.getTimeStamp();

	// Time formatting
	Date d = threadlocalDate.get();
	if ( d == null )
	{
	    d = new Date();
	    threadlocalDate.set( d );
	}

	d.setTime( lastUpdateTime );

	SimpleDateFormat dateFormat = threadlocalDateFormatter.get();
	if ( dateFormat == null )
	{
	    dateFormat = new SimpleDateFormat( PATTERN );
	    threadlocalDateFormatter.set( dateFormat );
	}

	String formattedTime = dateFormat.format( d );

	//
	// Trade type
	//
	TradeType tradeType = tradeEvent.getTradeType();
	
	//
	// Order Qty and exeuted qty 
	//
	double quantity = tradeEvent.getQuantity();
	String qtyAsStr = new BigDecimal( quantity ).setScale( 4, RoundingMode.HALF_EVEN ).toString();
	double executedQty = tradeEvent.getExecutedQty();
	String executedQtyStr = new BigDecimal(executedQty  ).setScale( 4, RoundingMode.HALF_EVEN ).toString();
	
	//
	// Client info
	//
	String firm = tradeEvent.getFirm();
	String counterFirm = tradeEvent.getCounterFirm();
	
	
	//
	// Status
	//
	Status status = tradeEvent.getStatus();
	
	Vector< Object > row = new Vector<>();
	row.add( tradeEvent.getEventId() );
	row.add( symbol );
	row.add( tradeType.toString() );
	row.add( side.toString() );
	row.add( qtyAsStr );
	row.add( executedQtyStr );
	row.add( priceAsStr );
	row.add( firm );
	row.add( counterFirm );
	row.add( status );
	row.add( formattedTime );
	
	addRow( row );
    }

}
