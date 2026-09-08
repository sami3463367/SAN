package net.san.offline;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.Assert.*;
public class RecordFilesTest {
    @Test public void persistsAndReturnsIndependentCopies() throws Exception {
        File dir=Files.createTempDirectory("san-rms").toFile();RecordFiles store=RecordFiles.open(dir,"saintsrow2",true);
        assertEquals(1,store.add(new byte[]{29,4,8},0,3));store.close();store=RecordFiles.open(dir,"saintsrow2",false);
        byte[] bytes=store.get(1);bytes[0]=0;assertArrayEquals(new byte[]{29,4,8},store.get(1));store.close();
    }
    @Test public void originalDeleteThenAddReplacesRecordOne() throws Exception {
        File dir=Files.createTempDirectory("san-rms").toFile();RecordFiles s=RecordFiles.open(dir,"saintsrow2",true);s.add(new byte[]{1},0,1);s.close();
        RecordFiles.delete(dir,"saintsrow2");s=RecordFiles.open(dir,"saintsrow2",true);assertEquals(1,s.add(new byte[]{2},0,1));s.close();
        assertArrayEquals(new byte[]{2},RecordFiles.open(dir,"saintsrow2",false).get(1));assertFalse(new File(dir,"saintsrow2.rms.bak").exists());
    }
    @Test(expected=IOException.class) public void rejectsTraversal() throws Exception {RecordFiles.open(Files.createTempDirectory("san-rms").toFile(),"../outside",true);}
    @Test(expected=IOException.class) public void closedStoreCannotRead() throws Exception {RecordFiles s=RecordFiles.open(Files.createTempDirectory("san-rms").toFile(),"save",true);s.close();s.get(1);}
}
