package ns.freetime.businessprocessor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public class MainUICanvas
{
    private final JPanel canvas;
    private QuotesTab quotesTab;
    private TradesTab tradesTab;

    public MainUICanvas()
    {
	JLabel lblHeading = new JLabel( "Run the sample market data source provided under /samples to see some data in this UI" );
	lblHeading.setFont( new Font( "Arial", Font.TRUETYPE_FONT, 12 ) );
	lblHeading.setForeground( Color.BLUE );
	
	JTabbedPane tabbedPane = new JTabbedPane();
	quotesTab = new QuotesTab();
	tabbedPane.addTab( "Quotes", quotesTab.getContent() );
	
	tradesTab = new TradesTab();
	tabbedPane.addTab( "Trades", tradesTab.getContent() );

	canvas = new JPanel();
	canvas.setLayout( new BorderLayout() );
	canvas.add( lblHeading, BorderLayout.PAGE_START );
	canvas.add( tabbedPane, BorderLayout.CENTER );
    }

    public Component getContent()
    {
	return canvas;
    }
    
    public void marketQuoteReceived( MarketEvent quoteEvent )
    {
	quotesTab.marketQuoteReceived( quoteEvent );
    }
    
    public void tradeEventReceived( MarketEvent tradeEvent )
    {
	tradesTab.tradeEventReceived( tradeEvent );
    }
    
}
