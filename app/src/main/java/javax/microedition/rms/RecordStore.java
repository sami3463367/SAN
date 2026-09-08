package javax.microedition.rms;
import java.io.File;
import java.io.IOException;
import net.san.offline.GameRuntime;
import net.san.offline.RecordFiles;
public final class RecordStore {
    private final RecordFiles files;
    private RecordStore(RecordFiles files) {this.files=files;}
    private static File directory() {return new File(GameRuntime.context().getFilesDir(),"rms");}
    public static RecordStore openRecordStore(String name,boolean create) throws IOException {return new RecordStore(RecordFiles.open(directory(),name,create));}
    public static void deleteRecordStore(String name) throws IOException {RecordFiles.delete(directory(),name);}
    public int addRecord(byte[] data,int offset,int length) throws IOException {return files.add(data,offset,length);}
    public byte[] getRecord(int id) throws IOException {return files.get(id);}
    public void closeRecordStore() {files.close();}
}
