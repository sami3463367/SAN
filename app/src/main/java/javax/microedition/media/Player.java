package javax.microedition.media;
public interface Player {
    int CLOSED=0,UNREALIZED=100,REALIZED=200,PREFETCHED=300,STARTED=400;
    void addPlayerListener(PlayerListener listener);
    void close();
    void deallocate();
    Control getControl(String name);
    int getState();
    void prefetch() throws MediaException;
    void realize() throws MediaException;
    void setLoopCount(int count);
    long setMediaTime(long microseconds) throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
}
