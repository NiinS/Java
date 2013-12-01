package ns.freetime.pipe;

import java.util.List;

import ns.freetime.businessprocessor.IMarketEventListener;

public interface IMarketEventWheel
{
    void startRotating();
    
    void stopRotating();
    
    void registerMarketEventListener(List<IMarketEventListener> eventListeners);

    void pushToWheel( byte[ ] array, int offset, int length );
}
