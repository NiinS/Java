package ns.freetime.proto;

import ns.freetime.proto.MarketDataProto.MarketEvent;
import ns.freetime.proto.MarketDataProto.MarketEvent.Builder;

import com.lmax.disruptor.EventFactory;

public class MarketEventBuilderFactory implements EventFactory< MarketEvent.Builder >
{
    public Builder newInstance()
    {
	return MarketEvent.newBuilder();
    }

}
