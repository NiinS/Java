package ns.freetime.businessprocessor;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public interface IMarketEventListener
{
    void onEvent(MarketEvent event);
}
