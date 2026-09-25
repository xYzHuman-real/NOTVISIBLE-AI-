package ai.notvisible.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.speech.*;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMPORT=10, PICK_TEXT=11, CREATE_EXPORT=12, REQ_AUDIO=13;
    private LinearLayout messages;
    private EditText input;
    private TextView status, title;
    private SharedPreferences prefs;
    private JSONArray history = new JSONArray();
    private String pendingFileContext = "";
    private SpeechRecognizer speech;
    private boolean busy = false;

    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView label(String s,int size,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(20,20,20));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setPadding(dp(14),dp(9),dp(14),dp(9)); return v;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("nv",MODE_PRIVATE);
        loadHistory(); buildUi();
        if(speechAvailable()) setupSpeech();
    }

    @Override protected void onDestroy(){ if(speech!=null)speech.destroy(); super.onDestroy(); }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(8),dp(5),dp(8),dp(2));
        title=label(chatTitle(),19,true); top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        Button menu=new Button(this); menu.setText("Menu"); menu.setOnClickListener(v->showMenu()); top.addView(menu,new LinearLayout.LayoutParams(dp(90),dp(52))); root.addView(top);
        status=label("Ready",12,false); status.setTextColor(Color.DKGRAY); root.addView(status);

        ScrollView scroll=new ScrollView(this); messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(dp(7),dp(7),dp(7),dp(7)); scroll.addView(messages);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout composer=new LinearLayout(this); composer.setPadding(dp(7),dp(5),dp(7),dp(9));
        Button attach=new Button(this); attach.setText("+"); attach.setOnClickListener(v->pickText());
        input=new EditText(this); input.setHint("Message…"); input.setGravity(Gravity.TOP); input.setMinLines(2); input.setMaxLines(5);
        Button mic=new Button(this); mic.setText("Mic"); mic.setOnClickListener(v->startVoice());
        Button send=new Button(this); send.setText("Send"); send.setOnClickListener(v->sendMessage());
        composer.addView(attach,new LinearLayout.LayoutParams(dp(48),dp(62)));
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(62),1));
        composer.addView(mic,new LinearLayout.LayoutParams(dp(62),dp(62)));
        composer.addView(send,new LinearLayout.LayoutParams(dp(76),dp(62))); root.addView(composer);
        setContentView(root); renderHistory();
    }

    private String chatTitle(){ return prefs.getString("title","New chat"); }

    private void renderHistory(){
        messages.removeAllViews();
        if(history.length()==0){ addBubble("NV-0.2","Welcome to NOTVISIBLEAI. Configure your NOTVISIBLEAI API in Menu → Settings to start."); return; }
        for(int i=0;i<history.length();i++){ try{ JSONObject m=history.getJSONObject(i); addBubble(m.getString("role"),m.getString("content")); }catch(Exception ignored){} }
    }

    private void addBubble(String who,String body){
        String name=who.equals("user")?"You":who.equals("assistant")?"NV-0.2":who;
        TextView v=label(name+"\n"+body,15,who.equals("user"));
        v.setBackgroundColor(who.equals("user")?Color.rgb(245,245,245):Color.rgb(250,250,250));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); messages.addView(v,p);
        v.setOnLongClickListener(x->{ if(who.equals("assistant")) copyText(body); return true; });
    }

    private void append(String role,String content){
        try{ JSONObject m=new JSONObject(); m.put("role",role); m.put("content",content); history.put(m); saveHistory(); addBubble(role,content); }catch(Exception ignored){}
    }

    private void saveHistory(){ prefs.edit().putString("history",history.toString()).apply(); }
    private void loadHistory(){
        try{ String s=prefs.getString("history",""); if(!s.isEmpty()) history=new JSONArray(s); }catch(Exception e){history=new JSONArray();}
    }

    private void showMenu(){
        String[] items={"New chat","Rename chat","Clear conversation","Copy last response","Regenerate last response","Export chat","Import chat","Attach text file","Settings"};
        new AlertDialog.Builder(this).setTitle("NOTVISIBLEAI").setItems(items,(d,w)->{
            switch(w){
                case 0:newChat();break; case 1:renameChat();break; case 2:clearChat();break; case 3:copyLast();break; case 4:regenerate();break;
                case 5:exportChat();break; case 6:importChat();break; case 7:pickText();break; case 8:showSettings();break;
            }
        }).show();
    }

    private void newChat(){ history=new JSONArray(); pendingFileContext=""; prefs.edit().putString("history","").putString("title","New chat").apply(); title.setText("New chat"); renderHistory(); status.setText("New conversation"); }
    private void clearChat(){ new AlertDialog.Builder(this).setTitle("Clear conversation?").setMessage("This removes the current local conversation.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->newChat()).show(); }

    private void renameChat(){
        final EditText e=new EditText(this); e.setSingleLine(); e.setText(chatTitle()); e.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Rename chat").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            String s=e.getText().toString().trim(); if(s.isEmpty())s="New chat"; prefs.edit().putString("title",s).apply(); title.setText(s);
        }).show();
    }

    private void copyText(String s){ ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("NOTVISIBLEAI",s)); status.setText("Copied"); }
    private void copyLast(){
        for(int i=history.length()-1;i>=0;i--)try{JSONObject m=history.getJSONObject(i);if(m.getString("role").equals("assistant")){copyText(m.getString("content"));return;}}catch(Exception ignored){}
        status.setText("No response to copy");
    }

    private void regenerate(){
        if(busy)return;
        try{
            int lastUser=-1; for(int i=history.length()-1;i>=0;i--)if(history.getJSONObject(i).getString("role").equals("user")){lastUser=i;break;}
            if(lastUser<0){status.setText("No user message");return;}
            while(history.length()>lastUser+1)history.remove(history.length()-1);
            saveHistory(); renderHistory(); sendConversation(false);
        }catch(Exception e){status.setText("Could not regenerate");}
    }

    private void sendMessage(){
        String q=input.getText().toString().trim(); if(q.isEmpty()||busy)return;
        if(prefs.getString("base","").isEmpty()){showSettings();return;}
        if(!pendingFileContext.isEmpty()) q += "\n\n[Attached text]\n"+pendingFileContext;
        append("user",q); input.setText(""); pendingFileContext=""; sendConversation(true);
    }

    private void sendConversation(boolean renderUser){
        String base=prefs.getString("base",""); if(base.isEmpty()){status.setText("API not configured");return;}
        busy=true; status.setText("Thinking…");
        final String model=prefs.getString("model","nv-0.2");
        final double temp=Double.parseDouble(prefs.getString("temperature","0.7"));
        final int max=Integer.parseInt(prefs.getString("max_tokens","1024"));
        final String memory=prefs.getString("memory","");
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                JSONObject payload=new JSONObject(); payload.put("model",model); payload.put("temperature",temp); payload.put("max_tokens",max);
                JSONArray msgs=new JSONArray();
                if(!memory.trim().isEmpty()){JSONObject sys=new JSONObject();sys.put("role","system");sys.put("content",memory);msgs.put(sys);}
                int start=Math.max(0,history.length()-40);
                for(int i=start;i<history.length();i++)msgs.put(history.getJSONObject(i));
                payload.put("messages",msgs);
                HttpURLConnection c=(HttpURLConnection)new URL(base+"/v1/chat/completions").openConnection();
                c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(180000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json");
                String key=prefs.getString("key",""); if(!key.isEmpty())c.setRequestProperty("Authorization","Bearer "+key);
                OutputStream os=c.getOutputStream();os.write(payload.toString().getBytes(StandardCharsets.UTF_8));os.close();
                int code=c.getResponseCode(); InputStream is=code>=400?c.getErrorStream():c.getInputStream(); BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8));
                StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();String body=sb.toString();
                if(code>=400)throw new IOException("HTTP "+code);
                String answer=new JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                runOnUiThread(()->{append("assistant",answer);status.setText("Ready");busy=false;});
            }catch(Exception e){runOnUiThread(()->{status.setText("API error");addBubble("assistant","Connection error: "+e.getMessage());busy=false;});}
        });
    }

    private void showSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),0,dp(8),0);
        EditText base=field("API base URL",prefs.getString("base","")); EditText key=field("API key (optional)",prefs.getString("key",""));key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText model=field("Model",prefs.getString("model","nv-0.2")); EditText temp=field("Temperature",prefs.getString("temperature","0.7")); EditText max=field("Max tokens",prefs.getString("max_tokens","1024")); EditText mem=field("Local memory / system instruction",prefs.getString("memory","")); mem.setMinLines(3);
        box.addView(base);box.addView(key);box.addView(model);box.addView(temp);box.addView(max);box.addView(mem);
        new AlertDialog.Builder(this).setTitle("Settings").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            prefs.edit().putString("base",base.getText().toString().trim().replaceAll("/$","")).putString("key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).putString("temperature",validTemp(temp.getText().toString())).putString("max_tokens",validMax(max.getText().toString())).putString("memory",mem.getText().toString()).apply();status.setText("Settings saved");
        }).show();
    }

    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine(false);return e;}
    private String validTemp(String s){try{double x=Double.parseDouble(s);if(x<0)x=0;if(x>2)x=2;return String.valueOf(x);}catch(Exception e){return "0.7";}}
    private String validMax(String s){try{int x=Integer.parseInt(s);if(x<1)x=1;if(x>8192)x=8192;return String.valueOf(x);}catch(Exception e){return "1024";}}

    private void exportChat(){
        try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,chatTitle().replaceAll("[^a-zA-Z0-9._-]","_")+".json");startActivityForResult(i,CREATE_EXPORT);}catch(Exception e){status.setText("Export unavailable");}
    }
    private void importChat(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMPORT);}

    private void pickText(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_TEXT);}

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data); if(result!=RESULT_OK||data==null)return;
        Uri u=data.getData(); if(u==null)return;
        try{
            if(request==PICK_IMPORT){String s=readUri(u);history=new JSONArray(s);saveHistory();renderHistory();status.setText("Chat imported");}
            else if(request==CREATE_EXPORT){OutputStream os=getContentResolver().openOutputStream(u);os.write(history.toString(2).getBytes(StandardCharsets.UTF_8));os.close();status.setText("Chat exported");}
            else if(request==PICK_TEXT){String s=readUri(u);if(s.length()>30000)s=s.substring(0,30000);pendingFileContext=s;status.setText("Text attached");}
        }catch(Exception e){status.setText("File operation failed");}
    }

    private String readUri(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();String l;while((l=br.readLine())!=null)b.append(l).append('\n');br.close();return b.toString();}

    private boolean speechAvailable(){return SpeechRecognizer.isRecognitionAvailable(this);}
    private void setupSpeech(){
        speech=SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){status.setText("Listening…");} public void onRmsChanged(float x){} public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){status.setText("Processing voice…");} public void onError(int e){status.setText("Voice input unavailable");}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){input.setText(r.get(0));input.setSelection(input.length());status.setText("Voice added");}}
            public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
        });
    }
    private void startVoice(){
        if(speech==null){status.setText("Speech recognition unavailable");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);speech.startListening(i);
    }
}
