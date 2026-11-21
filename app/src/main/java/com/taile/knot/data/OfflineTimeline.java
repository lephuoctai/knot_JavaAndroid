package com.taile.knot.data;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** App-private, excluded from backup; caller guards the captured session before writing. */
final class OfflineTimeline {
    private final File root;
    OfflineTimeline(Context context,String identity) {
        root=new File(context.getNoBackupFilesDir(),"timeline/"+digest(identity));
    }
    static String digest(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format("%02x",b&255));return out.toString();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    JSONObject read() {
        try(InputStream in=new AtomicFile(new File(root,"timeline.json")).openRead()) {
            return new JSONObject(new String(readBounded(in,4*1024*1024),StandardCharsets.UTF_8));
        }catch(Exception e){return null;}
    }
    void write(JSONObject snapshot) throws IOException {
        root.mkdirs();AtomicFile file=new AtomicFile(new File(root,"timeline.json"));FileOutputStream out=null;
        try {out=file.startWrite();out.write(snapshot.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
    }
    File image(String id) {
        if(!id.matches("[a-fA-F0-9-]{36}"))throw new IllegalArgumentException("Invalid image id");
        return new File(root,id+".jpg");
    }
    void saveImage(String id,byte[] bytes) throws IOException {
        root.mkdirs();AtomicFile file=new AtomicFile(image(id));FileOutputStream out=null;
        try {out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
    }
    void prune(java.util.Set<String> ids) {
        File[] files=root.listFiles();if(files!=null)for(File f:files)
            if(f.getName().endsWith(".jpg")&&!ids.contains(f.getName().replace(".jpg","")))f.delete();
    }
    static byte[] readBounded(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Response too large");out.write(b,0,n);}return out.toByteArray();
    }
    static void clear(Context context) {
        remove(new File(context.getNoBackupFilesDir(),"timeline"));
        remove(new File(context.getFilesDir(),"offline")); // Remove incomplete P20 cache too.
    }
    private static void remove(File f) {
        File[] children=f.listFiles();if(children!=null)for(File child:children)remove(child);f.delete();
    }
}
