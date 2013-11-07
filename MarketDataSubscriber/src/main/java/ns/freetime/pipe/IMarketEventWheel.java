package ns.freetime.pipe;

import ns.freetime.processor.IMarketEventListener;
import ns.freetime.proto.MarketDataProto.MarketEvent;

public interface IMarketEventWheel
{
    void startRotating();
    
    void stopRotating();
    
    void pushToWheel(MarketEvent event);
    
    void registerMarketEventListener(IMarketEventListener eventListener);
}
