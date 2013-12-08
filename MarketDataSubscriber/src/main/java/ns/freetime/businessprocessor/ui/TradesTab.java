package ns.freetime.businessprocessor.ui;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public class TradesTab
{

    private JScrollPane scrollPane;
    private TradeTableModel tradeTableModel;

    public TradesTab()
    {
	tradeTableModel = new TradeTableModel();
	JTable table = new JTable( tradeTableModel);
	scrollPane = new JScrollPane( table );
	table.setFillsViewportHeight( true );

    }

    public Component getContent()
    {
	return scrollPane;
    }

    public void tradeEventReceived( MarketEvent tradeEvent )
    {
	tradeTableModel.tradeEventReceived(tradeEvent);
    }
}
