package net.san.offline;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class TouchKeysTest {
    @Test public void supportsMovementAndFireAndIndependentRelease(){
        List<String> events=new ArrayList<>();TouchKeys keys=new TouchKeys((k,d)->events.add(k+":"+d));
        keys.update(7,'2','6');keys.update(31,'5');assertTrue(keys.isDown('2'));assertTrue(keys.isDown('5'));
        keys.release(7);assertFalse(keys.isDown('2'));assertTrue(keys.isDown('5'));keys.releaseAll();assertFalse(keys.isDown('5'));assertEquals(6,events.size());
    }
    @Test public void twoPointersSharingKeyDoNotReleaseEachOther(){
        List<String> events=new ArrayList<>();TouchKeys keys=new TouchKeys((k,d)->events.add(k+":"+d));
        keys.update(5,'5');keys.update(82,'5');keys.release(5);assertTrue(keys.isDown('5'));assertEquals(1,events.size());keys.release(82);assertEquals(2,events.size());
    }
    @Test public void slidingDirectionReleasesOldKey(){
        TouchKeys keys=new TouchKeys((k,d)->{});keys.update(9,'2');keys.update(9,'6');assertFalse(keys.isDown('2'));assertTrue(keys.isDown('6'));keys.releaseAll();assertFalse(keys.isDown('6'));
    }
}
