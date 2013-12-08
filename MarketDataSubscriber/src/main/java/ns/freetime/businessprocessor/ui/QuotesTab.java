package ns.freetime.businessprocessor.ui;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public class QuotesTab
{
    private JScrollPane scrollPane;
    private QuoteTableModel quoteTableModel;

    public QuotesTab()
    {
	quoteTableModel = new QuoteTableModel();

	JTable table = new JTable( quoteTableModel );

	scrollPane = new JScrollPane( table );
	table.setFillsViewportHeight( true );
    }

    public Component getContent()
    {
	return scrollPane;
    }

    public void marketQuoteReceived( MarketEvent quoteEvent )
    {
	quoteTableModel.marketQuoteReceived( quoteEvent );
    }

 }
