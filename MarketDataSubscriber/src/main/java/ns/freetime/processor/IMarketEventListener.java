package ns.freetime.processor;

import ns.freetime.proto.MarketDataProto.MarketEvent;

public interface IMarketEventListener
{
    void onEvent(MarketEvent event);
}
