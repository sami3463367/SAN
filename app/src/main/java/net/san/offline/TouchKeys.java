package net.san.offline;
import java.util.*;
/** Pointer IDs (not pointer indices) own sets of keys; key releases are reference counted. */
public final class TouchKeys {
    public interface Sink { void key(int code,boolean down); }
    private final Map<Integer,Set<Integer>> pointers=new HashMap<>();
    private final Map<Integer,Integer> counts=new HashMap<>();
    private final Sink sink;
    public TouchKeys(Sink sink){this.sink=sink;}
    public void update(int pointer,int... keys) {
        Set<Integer> next=new HashSet<>();for(int key:keys)next.add(key);
        Set<Integer> old=pointers.get(pointer);if(old==null)old=Collections.emptySet();
        for(int key:old)if(!next.contains(key)) {
            int n=counts.get(key)-1;if(n==0){counts.remove(key);sink.key(key,false);}else counts.put(key,n);
        }
        for(int key:next)if(!old.contains(key)) {
            int n=counts.containsKey(key)?counts.get(key):0;counts.put(key,n+1);if(n==0)sink.key(key,true);
        }
        if(next.isEmpty())pointers.remove(pointer);else pointers.put(pointer,next);
    }
    public void release(int pointer){update(pointer);}
    public void releaseAll(){for(int pointer:new ArrayList<>(pointers.keySet()))release(pointer);}
    public boolean isDown(int code){return counts.containsKey(code);}
}
