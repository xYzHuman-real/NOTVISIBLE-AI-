package ai.notvisible.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.content.SharedPreferences;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout messages;
    private EditText input;
    private TextView status;
    private SharedPreferences prefs;
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private TextView label(String s,int size,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(Color.rgb(20,20,20)); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); v.setPadding(dp(16),dp(10),dp(16),dp(10)); return v; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("nv",MODE_PRIVATE); buildUi(); }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(12),dp(8),dp(8),dp(4));
        top.addView(label("NOTVISIBLEAI",20,true),new LinearLayout.LayoutParams(0,dp(54),1));
        Button settings=new Button(this); settings.setText("Settings"); settings.setOnClickListener(v->showSettings()); top.addView(settings,new LinearLayout.LayoutParams(dp(105),dp(54))); root.addView(top);
        status=label("Not connected",13,false); root.addView(status);
        ScrollView scroll=new ScrollView(this); messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(dp(8),dp(8),dp(8),dp(8)); scroll.addView(messages); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=new LinearLayout(this); composer.setPadding(dp(8),dp(6),dp(8),dp(10)); input=new EditText(this); input.setHint("Message NV-0.2…"); Button send=new Button(this); send.setText("Send"); send.setOnClickListener(v->sendMessage()); composer.addView(input,new LinearLayout.LayoutParams(0,dp(60),1)); composer.addView(send,new LinearLayout.LayoutParams(dp(90),dp(60))); root.addView(composer); setContentView(root);
        addMessage("NV-0.2","Welcome to NOTVISIBLEAI. Add your NOTVISIBLEAI API endpoint in Settings to start chatting.");
    }

    private void addMessage(String who,String body){ TextView v=label(who+"\n"+body,15,who.equals("You")); v.setBackgroundColor(who.equals("You")?Color.rgb(245,245,245):Color.rgb(250,250,250)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(5),0,dp(5)); messages.addView(v,p); }

    private void showSettings(){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),0,dp(8),0);
        EditText base=new EditText(this); base.setHint("API base URL (https://…)"); base.setText(prefs.getString("base",""));
        EditText key=new EditText(this); key.setHint("API key (optional)"); key.setText(prefs.getString("key","")); key.setInputType(129); box.addView(base); box.addView(key);
        new android.app.AlertDialog.Builder(this).setTitle("NOTVISIBLEAI connection").setView(box).setPositiveButton("Save",(d,w)->{ prefs.edit().putString("base",base.getText().toString().trim().replaceAll("/$","" )).putString("key",key.getText().toString().trim()).apply(); status.setText("Connection saved"); }).setNegativeButton("Cancel",null).show();
    }

    private void sendMessage(){
        String q=input.getText().toString().trim(); if(q.isEmpty())return; String base=prefs.getString("base",""); if(base.isEmpty()){showSettings();return;} String key=prefs.getString("key",""); addMessage("You",q); input.setText(""); status.setText("Thinking…");
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                JSONObject payload=new JSONObject(); payload.put("model","nv-0.2"); JSONArray msgs=new JSONArray(); JSONObject m=new JSONObject(); m.put("role","user"); m.put("content",q); msgs.put(m); payload.put("messages",msgs); payload.put("temperature",0.7); payload.put("max_tokens",512);
                HttpURLConnection c=(HttpURLConnection)new URL(base+"/v1/chat/completions").openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(120000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json"); if(!key.isEmpty())c.setRequestProperty("Authorization","Bearer "+key);
                OutputStream os=c.getOutputStream(); os.write(payload.toString().getBytes("UTF-8")); os.close(); int code=c.getResponseCode(); InputStream is=code>=400?c.getErrorStream():c.getInputStream(); BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8")); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null)sb.append(line); br.close(); String body=sb.toString(); if(code>=400)throw new IOException("HTTP "+code+": "+body);
                String answer=new JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"); runOnUiThread(()->{addMessage("NV-0.2",answer);status.setText("Ready");});
            }catch(Exception e){runOnUiThread(()->{addMessage("NV-0.2","Connection error: "+e.getMessage());status.setText("API unavailable");});}
        });
    }
}
