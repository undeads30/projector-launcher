package com.gathin.tvhome;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.lang.Process;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;
import org.vosk.Model;
import org.vosk.Recognizer;

public class LocalVoiceService extends Service {
    private static final String TAG="ProjectorVoice";
    private static final String CHANNEL="local_voice";
    private static final int NOTIFICATION=418;
    private final VoiceSession session=new VoiceSession();
    private final ExecutorService decoder=Executors.newSingleThreadExecutor();
    private volatile boolean stopped,ready,working;
    private volatile long activeId;
    private volatile Process logger;
    private Model model;

    @Override public void onCreate(){
        super.onCreate();
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Голосовые команды",NotificationManager.IMPORTANCE_LOW));
        startForeground(NOTIFICATION,notification("Подготовка русского распознавания…"));
        if(!permissions()){VoiceActions.message(this,"Для голосовых команд нужны разрешения микрофона и журнала");stopSelf();return;}
        decoder.execute(()->{
            try {
                model=new Model(VoiceModelStore.prepare(this).getAbsolutePath());
                if(stopped)return;
                ready=true;manager.notify(NOTIFICATION,notification("Готово — удерживайте кнопку микрофона"));
                Log.i(TAG,"MODEL_READY local ARMv7");VoiceActions.message(this,"Голосовые команды готовы");
            }catch(Exception|LinkageError error){Log.e(TAG,"MODEL_FAILED",error);VoiceActions.message(this,"Не удалось загрузить русскую модель");stopSelf();}
        });
        new Thread(this::listen,"voice-button-log").start();
    }
    private boolean permissions(){return checkSelfPermission(Manifest.permission.READ_LOGS)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    private Notification notification(String text){
        PendingIntent settings=PendingIntent.getActivity(this,0,new Intent(this,VoiceSettingsActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getBroadcast(this,1,new Intent(this,VoiceActionReceiver.class).setAction("voice.stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Голосовые команды")
                .setContentText(text).setOngoing(true).setContentIntent(settings).addAction(new Notification.Action.Builder(null,"Выключить",stop).build()).build();
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
    @Override public IBinder onBind(Intent intent){return null;}

    private void listen(){
        int failures=0;
        while(!stopped&&failures<3){
            long listenerStarted=System.currentTimeMillis();
            int vendorPid=0;
            try{
                logger=new ProcessBuilder("/system/bin/logcat","-B","-b","main","-T","1","voice_hidraw:D","*:S").start();
                Log.i(TAG,"BUTTON_LISTENER_STARTED");
                try(InputStream input=logger.getInputStream()){
                    byte[] prefix=new byte[4];
                    while(!stopped){
                        readFully(input,prefix,0,4);
                        ByteBuffer header=ByteBuffer.wrap(prefix).order(ByteOrder.LITTLE_ENDIAN);
                        int length=header.getShort(0)&65535,size=header.getShort(2)&65535;
                        if(size<20||size>64||length>8192)throw new IOException("Invalid log frame");
                        byte[] frame=new byte[size+length];System.arraycopy(prefix,0,frame,0,4);readFully(input,frame,4,frame.length-4);
                        int event=VoiceLogEvent.parse(frame,listenerStarted,System.currentTimeMillis());
                        if(event==VoiceLogEvent.NONE)continue;
                        int pid=ByteBuffer.wrap(frame).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
                        if(vendorPid!=0&&pid!=vendorPid){session.cancel();activeId=0;}
                        vendorPid=pid;failures=0;
                        if(event==VoiceLogEvent.DOWN)down();else session.release(activeId,SystemClock.elapsedRealtime());
                    }
                }
            }catch(IOException error){if(!stopped){Log.w(TAG,"BUTTON_LISTENER_RETRY");failures++;}}
            finally{
                session.cancel();activeId=0;
                Process process=logger;if(process!=null){process.destroy();logger=null;}
            }
            if(!stopped)SystemClock.sleep(1000);
        }
        if(!stopped){VoiceActions.message(this,"Не удалось читать кнопку микрофона");stopSelf();}
    }
    private static void readFully(InputStream input,byte[] buffer,int offset,int count)throws IOException{
        while(count>0){int n=input.read(buffer,offset,count);if(n<0)throw new EOFException();offset+=n;count-=n;}
    }
    private void down(){
        if(stopped||!permissions())return;
        if(!ready){VoiceActions.message(this,"Русская модель ещё загружается");return;}
        if(working){VoiceActions.message(this,"Дождитесь завершения команды");return;}
        long id=session.start(SystemClock.elapsedRealtime());if(id==0)return;
        working=true;activeId=id;
        Log.i(TAG,"DOWN authenticated uid=0");VoiceActions.message(this,"Говорите…");
        String destination="";
        try {
            Bundle snapshot=getContentResolver().call(android.net.Uri.parse("content://com.gathin.tvhome.voice.context"),"foreground",null,null);
            if(snapshot!=null)destination=VoiceSearchQuery.destination(snapshot.getString("package"));
        }catch(Exception ignored){}
        final String target=destination;
        decoder.execute(()->capture(id,target));
    }
    private void capture(long id,String destination){
        AudioRecord audio=null;
        long started=SystemClock.elapsedRealtime(),samples=0;
        int peak=0;
        StringBuilder text=new StringBuilder();
        double confidenceTotal=0,minimum=1;int words=0;
        ByteArrayOutputStream recorded=new ByteArrayOutputStream(384000);
        try(Recognizer recognizer=new Recognizer(model,16000,new JSONArray(VoiceCommands.phrases()).toString())){
            recognizer.setWords(true);
            int buffer=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            audio=new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(buffer,6400));
            if(audio.getState()!=AudioRecord.STATE_INITIALIZED)throw new IOException("Audio initialization");
            audio.startRecording();
            short[] pcm=new short[320];
            java.util.ArrayList<JSONObject> pieces=new java.util.ArrayList<>();
            while(session.isRecording(id,SystemClock.elapsedRealtime())&&!stopped){
                int count=audio.read(pcm,0,pcm.length,AudioRecord.READ_NON_BLOCKING);
                if(count<0)throw new IOException("Audio read "+count);
                if(count==0){SystemClock.sleep(5);continue;}
                samples+=count;for(int i=0;i<count;i++)peak=Math.max(peak,Math.abs((int)pcm[i]));
                remember(recorded,pcm,count);
                if(recognizer.acceptWaveForm(pcm,count))pieces.add(new JSONObject(recognizer.getResult()));
            }
            if(!session.isProcessing(id)||stopped){VoiceActions.message(this,"Запись отменена");return;}
            // Drain only already buffered tail audio, bounded to 100 ms of PCM.
            int drained=0;
            while(drained<1600){
                int count=audio.read(pcm,0,Math.min(pcm.length,1600-drained),AudioRecord.READ_NON_BLOCKING);
                if(count<=0)break;drained+=count;samples+=count;
                for(int i=0;i<count;i++)peak=Math.max(peak,Math.abs((int)pcm[i]));
                remember(recorded,pcm,count);
                if(recognizer.acceptWaveForm(pcm,count))pieces.add(new JSONObject(recognizer.getResult()));
            }
            audio.stop();audio.release();audio=null;
            pieces.add(new JSONObject(recognizer.getFinalResult()));
            for(JSONObject piece:pieces){
                String part=piece.optString("text","").trim();if(!part.isEmpty()){if(text.length()>0)text.append(' ');text.append(part);}
                JSONArray result=piece.optJSONArray("result");if(result!=null)for(int i=0;i<result.length();i++){
                    double confidence=result.getJSONObject(i).optDouble("conf",0);confidenceTotal+=confidence;minimum=Math.min(minimum,confidence);words++;
                }
            }
            double average=words==0?0:confidenceTotal/words;
            VoiceCommand command=(samples>=3200&&peak>=120)?VoiceCommands.accept(text.toString(),average,minimum):VoiceCommand.NONE;
            // Decode the same bounded PCM locally, without the fixed command vocabulary.
            String freeText="";
            double freeAverage=0;
            if(samples>=3200&&peak>=120&&session.isProcessing(id)) {
                VoiceActions.message(this,"Распознаю…");
                try(Recognizer free=new Recognizer(model,16000)) {
                    free.setWords(true);
                    byte[] bytes=recorded.toByteArray();
                    StringBuilder transcript=new StringBuilder();
                    double sum=0;int count=0;
                    java.util.ArrayList<JSONObject> decoded=new java.util.ArrayList<>();
                    if(free.acceptWaveForm(bytes,bytes.length))decoded.add(new JSONObject(free.getResult()));
                    decoded.add(new JSONObject(free.getFinalResult()));
                    for(JSONObject piece:decoded){
                        String part=piece.optString("text","");
                        if(!part.isEmpty()){if(transcript.length()>0)transcript.append(' ');transcript.append(part);}
                        JSONArray details=piece.optJSONArray("result");
                        if(details!=null)for(int i=0;i<details.length();i++){sum+=details.getJSONObject(i).optDouble("conf",0);count++;}
                    }
                    freeText=transcript.toString();freeAverage=count==0?0:sum/count;
                }
            }
            String query=freeAverage>=.65?VoiceSearchQuery.extract(freeText):"";
            if(!query.isEmpty()) {
                Intent search=new Intent(this,VoiceActionReceiver.class).setAction("voice.search")
                        .putExtra("issued",SystemClock.elapsedRealtime()).putExtra("query",query).putExtra("destination",destination);
                if(!stopped&&session.publish(id,()->sendBroadcast(search)))Log.i(TAG,"FINAL search dispatched");
                return;
            }
            command=VoiceSearchQuery.command(freeText,command);
            if(stopped)return;
            if(command==VoiceCommand.NONE){
                if(session.complete(id)){Log.i(TAG,"FINAL command=NONE samples="+samples);VoiceActions.message(this,"Команда не распознана — повторите");}
                return;
            }
            Intent action=new Intent(this,VoiceActionReceiver.class).setAction("voice.result").putExtra("issued",SystemClock.elapsedRealtime()).putExtra("command",command.name());
            if(session.publish(id,()->sendBroadcast(action))){
                Log.i(TAG,"FINAL command="+command+" samples="+samples+" elapsed="+(SystemClock.elapsedRealtime()-started));
                VoiceActions.message(this,"Распознано: "+text);
            }
        }catch(Exception|LinkageError error){Log.e(TAG,"CAPTURE_FAILED",error);VoiceActions.message(this,"Не удалось распознать команду");}
        finally{
            if(audio!=null){try{audio.stop();}catch(IllegalStateException ignored){}audio.release();}
            session.cancel();working=false;
        }
    }
    private static void remember(ByteArrayOutputStream output,short[] pcm,int count) {
        if(output.size()+count*2>400000)throw new IllegalStateException("Capture bound exceeded");
        for(int i=0;i<count;i++){output.write(pcm[i]&255);output.write((pcm[i]>>>8)&255);}
    }
    @Override public void onDestroy(){
        stopped=true;ready=false;session.cancel();Process process=logger;if(process!=null)process.destroy();
        decoder.execute(()->{if(model!=null){model.close();model=null;}});decoder.shutdown();
        stopForeground(true);super.onDestroy();
    }
}
