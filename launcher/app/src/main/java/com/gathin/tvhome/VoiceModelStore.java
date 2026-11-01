package com.gathin.tvhome;
import android.content.Context;
import java.io.*;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
final class VoiceModelStore {
    private static final String HASH="961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4";
    static File prepare(Context context) throws Exception {
        File root=new File(context.getFilesDir(),"voice-model-ru-0.22");
        File marker=new File(root,".ready");
        if(marker.isFile()&&new File(root,"am/final.mdl").isFile())return root;
        File archive=new File(context.getCacheDir(),"voice-model.zip");
        File stage=new File(context.getFilesDir(),"voice-model-extract");
        remove(stage); if(!stage.mkdirs())throw new IOException("Model staging directory");
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[65536];
            try(InputStream input=context.getAssets().open("voice-model.zip");OutputStream output=new FileOutputStream(archive)) {
                int n;while((n=input.read(buffer))!=-1){digest.update(buffer,0,n);output.write(buffer,0,n);}
            }
            StringBuilder hex=new StringBuilder();for(byte value:digest.digest())hex.append(Integer.toHexString((value&255)|256).substring(1));
            if(!HASH.equals(hex.toString()))throw new IOException("Model checksum");
            long total=0;int files=0;
            String prefix=stage.getCanonicalPath()+File.separator;
            try(ZipInputStream zip=new ZipInputStream(new FileInputStream(archive))) {
                ZipEntry entry;while((entry=zip.getNextEntry())!=null) {
                    String name=entry.getName();String top="vosk-model-small-ru-0.22/";
                    if(!name.startsWith(top))throw new IOException("Model archive root");
                    name=name.substring(top.length());if(name.isEmpty())continue;
                    File file=new File(stage,name);
                    if(!file.getCanonicalPath().startsWith(prefix)||++files>128)throw new IOException("Model archive path");
                    if(entry.isDirectory()){file.mkdirs();continue;}
                    File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Model directory");
                    try(OutputStream out=new FileOutputStream(file)) {
                        int n;while((n=zip.read(buffer))!=-1){total+=n;if(total>200_000_000)throw new IOException("Model size");out.write(buffer,0,n);}
                    }
                }
            }
            if(!new File(stage,"am/final.mdl").isFile()||!new File(stage,"conf/model.conf").isFile())throw new IOException("Incomplete model");
            try(OutputStream out=new FileOutputStream(new File(stage,".ready"))){out.write(HASH.getBytes("US-ASCII"));}
            remove(root);if(!stage.renameTo(root))throw new IOException("Model commit");
            return root;
        } finally { archive.delete();remove(stage); }
    }
    private static void remove(File file) throws IOException {
        if(!file.exists())return;
        if(file.isDirectory()){File[] children=file.listFiles();if(children==null)throw new IOException("Model directory inaccessible");for(File child:children)remove(child);}
        if(!file.delete())throw new IOException("Model cleanup");
    }
}
