package net.san.offline;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Small transactional RMS backend. Stores remain inside Android's private files directory. */
public final class RecordFiles {
    private static final Set<String> deleted = new HashSet<>();
    private final File file;
    private final TreeMap<Integer,byte[]> records = new TreeMap<>();
    private boolean closed;
    private RecordFiles(File file) { this.file=file; }
    private static File path(File dir,String name) throws IOException {
        if(!name.matches("[A-Za-z0-9_.-]{1,32}") || name.equals(".") || name.equals("..")) throw new IOException("Invalid record store name");
        if(!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create save directory");
        return new File(dir,name+".rms");
    }
    public static synchronized RecordFiles open(File dir,String name,boolean create) throws IOException {
        File file=path(dir,name), backup=new File(file+".bak");
        boolean fresh=deleted.contains(file.getAbsolutePath());
        if(!fresh && !file.exists() && backup.exists()) move(backup,file);
        if((fresh || !file.exists()) && !create) throw new FileNotFoundException(name);
        RecordFiles store=new RecordFiles(file);
        if(!fresh && file.exists()) {
            try(DataInputStream in=new DataInputStream(new FileInputStream(file))) {
                if(in.readInt()!=0x53414E31) throw new IOException("Invalid save header");
                int count=in.readInt(); if(count<0 || count>10000) throw new IOException("Invalid save count");
                for(int i=0;i<count;i++) {
                    int id=in.readInt(),length=in.readInt();
                    if(id<1 || length<0 || length>16*1024*1024) throw new IOException("Invalid save record");
                    byte[] data=new byte[length]; in.readFully(data); store.records.put(id,data);
                }
            }
        }
        return store;
    }
    public static synchronized void delete(File dir,String name) throws IOException {
        File file=path(dir,name);
        if(file.exists()) move(file,new File(file+".bak"));
        deleted.add(file.getAbsolutePath());
    }
    public synchronized byte[] get(int id) throws IOException {
        check();byte[] value=records.get(id);
        if(value==null) throw new FileNotFoundException("Record "+id);
        return value.clone();
    }
    public synchronized int add(byte[] value,int offset,int length) throws IOException {
        check();
        if(offset<0 || length<0 || offset>value.length-length) throw new IndexOutOfBoundsException();
        int id=records.isEmpty()?1:records.lastKey()+1;
        records.put(id,Arrays.copyOfRange(value,offset,offset+length));
        File tmp=new File(file+".tmp");
        try(FileOutputStream raw=new FileOutputStream(tmp); DataOutputStream out=new DataOutputStream(raw)) {
            out.writeInt(0x53414E31);out.writeInt(records.size());
            for(Map.Entry<Integer,byte[]> e:records.entrySet()) {out.writeInt(e.getKey());out.writeInt(e.getValue().length);out.write(e.getValue());}
            out.flush();raw.getFD().sync();
        } catch(IOException error) {records.remove(id);throw error;}
        move(tmp,file);
        synchronized(RecordFiles.class) {deleted.remove(file.getAbsolutePath());}
        Files.deleteIfExists(new File(file+".bak").toPath());
        return id;
    }
    public synchronized void close() { closed=true; }
    private void check() throws IOException {if(closed)throw new IOException("Record store closed");}
    private static void move(File from,File to) throws IOException {
        try { Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch(AtomicMoveNotSupportedException e) {Files.move(from.toPath(),to.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    }
}
