package ai.notvisible.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.core.splashscreen.SplashScreen;
import org.json.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMPORT=10, PICK_FILE=11, CREATE_EXPORT=12, REQ_AUDIO=13;
    private final int INK=Color.rgb(244,247,252), MUTED=Color.rgb(158,171,194), BG=Color.rgb(7,11,20);
    private final int BLUE=Color.rgb(154,184,255), BLUE_SOFT=Color.rgb(32,46,76), CARD=Color.rgb(18,25,39), BORDER=Color.rgb(58,72,96);
    private LinearLayout root, content, bottom;
    private TextView pageTitle, status;
    private EditText input;
    private SharedPreferences prefs;
    private JSONArray history=new JSONArray();
    private String pendingFileContext="";
    private SpeechRecognizer speech;
    private boolean busy=false;
    private String screen="chat";

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView tv(String text,float size,int color,boolean bold){
        TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL); v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outline(int color,int stroke,int radius){GradientDrawable g=bg(color,radius);g.setStroke(dp(1),stroke);return g;}
    private Button btn(String text,boolean primary){
        Button b=new Button(this); b.setText(text); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?Color.WHITE:INK); b.setGravity(Gravity.CENTER); b.setPadding(dp(10),0,dp(10),0);
        b.setBackground(bg(primary?INK:Color.WHITE,16)); if(!primary)b.setBackground(outline(Color.WHITE,BORDER,16));
        return b;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView spacer(){return tv("",1,Color.TRANSPARENT,false);}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    private void addGap(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    @Override public void onCreate(Bundle b){
        SplashScreen.installSplashScreen(this);
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        prefs=getSharedPreferences("nv",MODE_PRIVATE);
        loadHistory();
        if(speechAvailable())setupSpeech();

        // One-time migration into the new premium first-run experience.
        if(!prefs.getBoolean("premium_flow_v1",false)){
            prefs.edit().remove("onboarded").remove("auth").putBoolean("premium_flow_v1",true).apply();
            showOnboarding(0);
        } else if(!prefs.getBoolean("auth",false)){
            showAuth(false);
        } else {
            enterApp();
        }
    }
    @Override protected void onDestroy(){if(speech!=null)speech.destroy();super.onDestroy();}

    private void buildShell(){
        root=col();root.setBackground(premiumBackground());
        root.addView(buildTopBar(),new LinearLayout.LayoutParams(-1,dp(72)));
        content=col();root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        bottom=buildBottomNav();root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(82)));
        setContentView(root);
    }
    private LinearLayout buildTopBar(){
        LinearLayout bar=row();pad(bar,18,12,16,8);
        pageTitle=tv(chatTitle(),20,Color.WHITE,true);
        bar.addView(pageTitle,new LinearLayout.LayoutParams(0,-1,1));
        TextView logo=tv("N",17,Color.rgb(8,14,26),true);logo.setGravity(Gravity.CENTER);logo.setBackground(bg(Color.WHITE,15));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(42),dp(42));lp.setMargins(0,0,dp(7),0);bar.addView(logo,lp);
        Button menu=btn("...",false);menu.setTextSize(12);menu.setOnClickListener(v->showMenu());
        bar.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(42)));return bar;
    }
    private LinearLayout buildBottomNav(){
        LinearLayout outer=row();pad(outer,14,7,14,9);
        LinearLayout nav=row();nav.setBackground(glass(20,35,24));pad(nav,5,5,5,5);
        String[] names={"Chats","Files","Settings"};String[] ids={"chat","files","settings"};
        for(int i=0;i<3;i++){final String id=ids[i];Button b=btn(names[i],false);b.setTextSize(12);b.setOnClickListener(v->showScreen(id));
            nav.addView(b,new LinearLayout.LayoutParams(0,dp(54),1));}
        outer.addView(nav,new LinearLayout.LayoutParams(-1,dp(64)));return outer;
    }
    private void showScreen(String id){
        screen=id;content.removeAllViews();bottom.setVisibility(id.equals("welcome")?View.GONE:View.VISIBLE);
        pageTitle.setText(id.equals("chat")?chatTitle():id.equals("files")?"Files":id.equals("settings")?"Settings":id.equals("about")?"About NOTVISIBLEAI":id.equals("transfer")?"Import / Export":"NOTVISIBLEAI");
        if(id.equals("chat"))renderChat(); else if(id.equals("files"))renderFiles(); else if(id.equals("settings"))renderSettings(); else if(id.equals("about"))renderAbout(); else if(id.equals("transfer"))renderTransfer(); else if(id.equals("error"))renderError();
    }

    private void enterApp(){
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildShell();
        showScreen("chat");
    }

    private GradientDrawable premiumBackground(){
        GradientDrawable g=new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5,9,18),Color.rgb(13,22,39),Color.rgb(27,38,65),Color.rgb(10,17,31)});
        return g;
    }

    private GradientDrawable glass(int fillAlpha,int strokeAlpha,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.argb(fillAlpha,255,255,255));
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1),Color.argb(strokeAlpha,255,255,255));
        return g;
    }

    private TextView whiteText(String text,float size,boolean bold){
        return tv(text,size,Color.WHITE,bold);
    }

    private LinearLayout premiumPage(){
        LinearLayout page=col();
        page.setBackground(premiumBackground());
        pad(page,22,24,22,24);
        return page;
    }

    private TextView brandMark(){
        TextView n=tv("N",30,Color.rgb(8,15,30),true);
        n.setGravity(Gravity.CENTER);
        n.setBackground(bg(Color.WHITE,20));
        return n;
    }

    private void showOnboarding(int pageIndex){
        LinearLayout page=premiumPage();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView top=whiteText("NOTVISIBLEAI",16,true);
        top.setGravity(Gravity.CENTER);
        page.addView(top,new LinearLayout.LayoutParams(-1,dp(40)));

        LinearLayout.LayoutParams markLp=new LinearLayout.LayoutParams(dp(78),dp(78));
        markLp.gravity=Gravity.CENTER_HORIZONTAL;
        TextView mark=brandMark();
        markLp.setMargins(0,dp(38),0,dp(28));
        page.addView(mark,markLp);

        String[] titles={"Intelligence, without the clutter.","Your private AI workspace.","Built around your flow."};
        String[] bodies={
                "A calm, premium space to think, create, learn and solve — without a noisy interface.",
                "Chat naturally, bring in files, use your voice and keep your workspace organized.",
                "Connect the model infrastructure you choose and keep your conversations under your control."
        };
        String[] mini={"THINK  •  CREATE  •  DISCOVER","CHAT  •  FILES  •  VOICE","PRIVATE  •  FLEXIBLE  •  YOURS"};

        Space flex=new Space(this);
        page.addView(flex,new LinearLayout.LayoutParams(1,0,1));

        LinearLayout glassCard=col();
        glassCard.setBackground(glass(24,46,30));
        pad(glassCard,22,24,22,22);
        TextView eyebrow=whiteText(mini[pageIndex],11,true);eyebrow.setAlpha(.78f);glassCard.addView(eyebrow);
        addGap(glassCard,13);
        TextView title=whiteText(titles[pageIndex],30,true);title.setLineSpacing(0,1.05f);glassCard.addView(title);
        addGap(glassCard,12);
        TextView body=whiteText(bodies[pageIndex],15,false);body.setAlpha(.82f);body.setLineSpacing(0,1.15f);glassCard.addView(body);
        page.addView(glassCard,new LinearLayout.LayoutParams(-1,dp(238)));

        addGap(page,24);
        LinearLayout dots=row();dots.setGravity(Gravity.CENTER);
        for(int i=0;i<3;i++){
            View dot=new View(this);
            dot.setBackground(bg(i==pageIndex?Color.WHITE:Color.argb(70,255,255,255),8));
            LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(i==pageIndex?28:8),dp(8));
            dl.setMargins(dp(4),0,dp(4),0);dots.addView(dot,dl);
        }
        page.addView(dots,new LinearLayout.LayoutParams(-1,dp(20)));

        addGap(page,16);
        Button next=btn(pageIndex==2?"Enter NOTVISIBLEAI":"Continue",true);
        next.setTextSize(15);
        next.setBackground(bg(Color.WHITE,18));
        next.setTextColor(Color.rgb(8,15,30));
        next.setOnClickListener(v->{
            if(pageIndex<2) showOnboarding(pageIndex+1);
            else {prefs.edit().putBoolean("intro_complete",true).apply();showAuth(false);}
        });
        page.addView(next,new LinearLayout.LayoutParams(-1,dp(56)));
        Button skip=btn("Skip intro",false);
        skip.setTextColor(Color.WHITE);skip.setBackground(glass(18,35,18));
        skip.setOnClickListener(v->{prefs.edit().putBoolean("intro_complete",true).apply();showAuth(false);});
        page.addView(skip,new LinearLayout.LayoutParams(-1,dp(48)));

        setContentView(page);
    }

    private boolean authSignup=false;
    private void showAuth(boolean signup){
        authSignup=signup;
        LinearLayout page=premiumPage();
        ScrollView sv=new ScrollView(this);
        LinearLayout box=col();pad(box,4,16,4,24);

        LinearLayout header=row();
        TextView mark=brandMark();
        header.addView(mark,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout ht=col();pad(ht,14,0,0,0);
        ht.addView(whiteText("NOTVISIBLEAI",20,true));
        TextView small=whiteText("A quieter place for intelligence.",12,false);small.setAlpha(.70f);ht.addView(small);
        header.addView(ht,new LinearLayout.LayoutParams(0,dp(62),1));
        box.addView(header);

        addGap(box,34);
        TextView title=whiteText(signup?"Create your account":"Welcome back",30,true);box.addView(title);
        addGap(box,7);
        TextView sub=whiteText(signup?"Start your private AI workspace.":"Sign in to continue to your AI workspace.",14,false);sub.setAlpha(.72f);box.addView(sub);
        addGap(box,22);

        LinearLayout card=col();card.setBackground(glass(24,45,28));pad(card,18,18,18,18);
        EditText name=null;
        if(signup){
            name=authField("Full name","Your name");
            card.addView(name,new LinearLayout.LayoutParams(-1,dp(54)));addGap(card,10);
        }
        EditText email=authField("Email","you@example.com");card.addView(email,new LinearLayout.LayoutParams(-1,dp(54)));addGap(card,10);
        EditText pass=authField("Password",signup?"Create a password":"Your password");
        pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(pass,new LinearLayout.LayoutParams(-1,dp(54)));addGap(card,14);

        Button submit=btn(signup?"Create account":"Sign in",true);
        submit.setTextSize(15);submit.setBackground(bg(Color.WHITE,16));submit.setTextColor(Color.rgb(8,15,30));
        EditText finalName=name;
        submit.setOnClickListener(v->{
            String em=email.getText().toString().trim().toLowerCase(Locale.US);
            String pw=pass.getText().toString();
            if(!em.contains("@")||em.length()<5){email.setError("Enter a valid email");return;}
            if(pw.length()<6){pass.setError("Use at least 6 characters");return;}
            if(authSignup){
                String n=finalName==null?"":finalName.getText().toString().trim();
                if(n.isEmpty()){finalName.setError("Enter your name");return;}
                byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
                prefs.edit().putString("auth_email",em).putString("auth_name",n)
                        .putString("auth_salt",hex(salt)).putString("auth_hash",hashPassword(pw,salt))
                        .putBoolean("auth",true).apply();
                enterApp();
            }else{
                String stored=prefs.getString("auth_email","");
                String saltHex=prefs.getString("auth_salt","");
                String hash=prefs.getString("auth_hash","");
                if(stored.isEmpty()||!stored.equals(em)||saltHex.isEmpty()||!hash.equals(hashPassword(pw,fromHex(saltHex)))){
                    statusToast("Email or password is incorrect");return;
                }
                prefs.edit().putBoolean("auth",true).apply();enterApp();
            }
        });
        card.addView(submit,new LinearLayout.LayoutParams(-1,dp(54)));
        box.addView(card);

        addGap(box,18);
        TextView divider=whiteText("  OR  ",11,false);divider.setGravity(Gravity.CENTER);divider.setAlpha(.55f);box.addView(divider,new LinearLayout.LayoutParams(-1,dp(28)));
        addGap(box,6);
        Button google=btn("Continue with Google",false);
        google.setTextSize(14);google.setTextColor(Color.WHITE);google.setBackground(glass(18,45,18));
        google.setOnClickListener(v->statusToast("Google sign-in will be connected to Credential Manager when the production identity backend is configured."));
        box.addView(google,new LinearLayout.LayoutParams(-1,dp(54)));

        addGap(box,18);
        TextView toggle=whiteText(signup?"Already have an account?  Sign in":"New to NOTVISIBLEAI?  Create an account",13,false);
        toggle.setGravity(Gravity.CENTER);toggle.setOnClickListener(v->showAuth(!authSignup));box.addView(toggle,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView privacy=whiteText("By continuing, you agree to the app's privacy and account terms.",11,false);privacy.setGravity(Gravity.CENTER);privacy.setAlpha(.55f);box.addView(privacy,new LinearLayout.LayoutParams(-1,dp(40)));

        sv.addView(box);page.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
    }

    private EditText authField(String label,String hint){
        EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.argb(145,255,255,255));
        e.setTextSize(14);e.setSingleLine(true);e.setBackground(glass(20,38,16));e.setPadding(dp(14),0,dp(14),0);
        e.setContentDescription(label);return e;
    }

    private String hashPassword(String password,byte[] salt){
        try{
            javax.crypto.SecretKeyFactory f=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),salt,120000,256);
            return hex(f.generateSecret(spec).getEncoded());
        }catch(Exception e){return "";}
    }
    private String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x & 0xff));return s.toString();}
    private byte[] fromHex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}

    private void showWelcome(){
        screen="welcome";content.removeAllViews();bottom.setVisibility(View.GONE);pageTitle.setText("");
        ScrollView sv=new ScrollView(this);LinearLayout box=col();pad(box,24,35,24,30);
        TextView mark=tv("N",54,Color.WHITE,true);mark.setGravity(Gravity.CENTER);mark.setBackground(bg(INK,28));
        LinearLayout.LayoutParams ml=new LinearLayout.LayoutParams(dp(100),dp(100));ml.gravity=Gravity.CENTER_HORIZONTAL;box.addView(mark,ml);
        addGap(box,26);TextView h=tv("NOTVISIBLEAI",32,INK,true);h.setGravity(Gravity.CENTER);box.addView(h,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView sub=tv("Your thoughts. Our intelligence.",16,MUTED,false);sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(32)));
        addGap(box,24);box.addView(card("A private AI workspace","Chat, reason, attach documents, use your voice, and keep conversations on your device.", "Built to connect to your NOTVISIBLEAI API."));
        addGap(box,18);
        LinearLayout features=col();features.addView(feature("✦","Chat & ask anything"));features.addView(feature("▣","Files & documents"));features.addView(feature("◉","Voice input"));features.addView(feature("⚙","Custom settings"));
        box.addView(features);
        addGap(box,22);Button start=btn("Get started  →",true);start.setTextSize(16);start.setOnClickListener(v->{prefs.edit().putBoolean("onboarded",true).apply();showScreen("chat");});
        box.addView(start,new LinearLayout.LayoutParams(-1,dp(56)));addGap(box,10);
        TextView note=tv("You control the API connection and model.",12,MUTED,false);note.setGravity(Gravity.CENTER);box.addView(note,new LinearLayout.LayoutParams(-1,dp(32)));
        sv.addView(box);content.addView(sv,new LinearLayout.LayoutParams(-1,-1));
    }
    private LinearLayout feature(String icon,String text){
        LinearLayout r=row();pad(r,4,8,4,8);TextView i=tv(icon,18,INK,true);i.setGravity(Gravity.CENTER);r.addView(i,new LinearLayout.LayoutParams(dp(38),dp(40)));
        r.addView(tv(text,15,INK,false),new LinearLayout.LayoutParams(0,dp(40),1));return r;
    }
    private LinearLayout card(String title,String body,String foot){
        LinearLayout c=col();c.setBackground(outline(Color.WHITE,BORDER,20));pad(c,18,15,18,15);
        c.addView(tv(title,17,INK,true));addGap(c,5);c.addView(tv(body,14,MUTED,false));addGap(c,8);c.addView(tv(foot,12,BLUE,false));return c;
    }

    private void renderChat(){
        ScrollView scroll=new ScrollView(this);LinearLayout list=col();pad(list,14,10,14,8);
        if(history.length()==0){
            list.addView(heroChat());addGap(list,18);
            LinearLayout quick=row();Button q1=btn("Explain something",false);q1.setOnClickListener(v->{input.setText("Explain this concept in simple words: ");input.requestFocus();});
            Button q2=btn("Brainstorm",false);q2.setOnClickListener(v->{input.setText("Help me brainstorm ideas for ");input.requestFocus();});
            quick.addView(q1,new LinearLayout.LayoutParams(0,48,1));quick.addView(q2,new LinearLayout.LayoutParams(0,48,1));list.addView(quick);
        } else {
            for(int i=0;i<history.length();i++)try{JSONObject m=history.getJSONObject(i);list.addView(messageCard(m.getString("role"),m.getString("content")));}catch(Exception ignored){}
        }
        scroll.addView(list);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=buildComposer();content.addView(composer,new LinearLayout.LayoutParams(-1,dp(84)));
    }
    private LinearLayout heroChat(){
        LinearLayout h=col();h.setGravity(Gravity.CENTER);pad(h,18,28,18,22);h.setBackground(outline(Color.WHITE,BORDER,24));
        TextView n=tv("N",30,Color.WHITE,true);n.setGravity(Gravity.CENTER);n.setBackground(bg(INK,18));h.addView(n,new LinearLayout.LayoutParams(dp(62),dp(62)));
        addGap(h,12);TextView t=tv("How can I help?",23,INK,true);t.setGravity(Gravity.CENTER);h.addView(t,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView s=tv("Ask a question, attach a file, or use your voice.",14,MUTED,false);s.setGravity(Gravity.CENTER);h.addView(s,new LinearLayout.LayoutParams(-1,dp(34)));return h;
    }
    private LinearLayout messageCard(String role,String text){
        boolean user=role.equals("user");LinearLayout c=col();pad(c,14,12,14,12);c.setBackground(bg(user?INK:Color.WHITE,18));
        TextView who=tv(user?"You":"NV-0.2",12,user?Color.WHITE:BLUE,true);c.addView(who);addGap(c,3);
        TextView body=tv(text,15,user?Color.WHITE:INK,false);body.setTextIsSelectable(true);c.addView(body);
        if(!user){body.setOnLongClickListener(v->{copyText(text);return true;});}
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));return wrap(c,p);
    }
    private LinearLayout wrap(LinearLayout v,LinearLayout.LayoutParams p){LinearLayout w=col();w.addView(v,p);return w;}
    private LinearLayout buildComposer(){
        LinearLayout c=row();c.setBackgroundColor(Color.WHITE);pad(c,8,7,8,8);
        Button attach=btn("+",false);attach.setTextSize(22);attach.setContentDescription("Attach file");attach.setOnClickListener(v->pickFile());
        c.addView(attach,new LinearLayout.LayoutParams(dp(48),dp(60)));
        input=new EditText(this);input.setHint("Message…");input.setTextSize(15);input.setGravity(Gravity.CENTER_VERTICAL);input.setSingleLine(false);input.setMaxLines(3);input.setPadding(dp(14),0,dp(10),0);input.setBackground(outline(Color.WHITE,BORDER,18));input.setContentDescription("Message");
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(60),1);ip.setMargins(dp(6),0,dp(6),0);c.addView(input,ip);
        Button mic=btn("◉",false);mic.setContentDescription("Voice input");mic.setOnClickListener(v->startVoice());c.addView(mic,new LinearLayout.LayoutParams(dp(50),dp(60)));
        Button send=btn("↑",true);send.setTextSize(22);send.setContentDescription("Send message");send.setOnClickListener(v->sendMessage());c.addView(send,new LinearLayout.LayoutParams(dp(50),dp(60)));
        return c;
    }

    private void renderFiles(){
        ScrollView sv=new ScrollView(this);LinearLayout box=col();pad(box,16,14,16,24);
        box.addView(card("Your files","Attach text documents to a conversation. Files are read locally by the app before being sent as message context.","Supported now: text files • Up to 30,000 characters per attachment."));
        addGap(box,14);
        Button add=btn("+  Attach a text file",true);add.setOnClickListener(v->pickFile());box.addView(add,new LinearLayout.LayoutParams(-1,52));
        addGap(box,20);box.addView(tv("Recent files",18,INK,true));addGap(box,8);
        String files=prefs.getString("files","");
        if(files.isEmpty()){box.addView(emptyCard("No files yet","Attach a document from your device to see it here."));}
        else for(String f:files.split("\\|"))if(!f.trim().isEmpty())box.addView(fileRow(f));
        sv.addView(box);content.addView(sv,new LinearLayout.LayoutParams(-1,-1));
    }
    private LinearLayout fileRow(String name){
        LinearLayout r=row();r.setBackground(outline(Color.WHITE,BORDER,18));pad(r,12,10,12,10);
        TextView ico=tv("▣",20,BLUE,true);ico.setGravity(Gravity.CENTER);r.addView(ico,new LinearLayout.LayoutParams(dp(42),dp(48)));
        LinearLayout t=col();t.addView(tv(name,14,INK,true));t.addView(tv("Text attachment",12,MUTED,false));r.addView(t,new LinearLayout.LayoutParams(0,dp(48),1));return wrap(r,new LinearLayout.LayoutParams(-1,dp(70)));
    }
    private LinearLayout emptyCard(String title,String body){
        LinearLayout c=col();c.setGravity(Gravity.CENTER);c.setBackground(outline(Color.WHITE,BORDER,20));pad(c,18,22,18,22);
        TextView i=tv("○",32,MUTED,false);i.setGravity(Gravity.CENTER);c.addView(i,new LinearLayout.LayoutParams(-1,45));
        TextView h=tv(title,16,INK,true);h.setGravity(Gravity.CENTER);c.addView(h,new LinearLayout.LayoutParams(-1,30));
        TextView b=tv(body,13,MUTED,false);b.setGravity(Gravity.CENTER);c.addView(b,new LinearLayout.LayoutParams(-1,48));return c;
    }

    private void renderSettings(){
        ScrollView sv=new ScrollView(this);LinearLayout box=col();pad(box,16,14,16,30);
        box.addView(sectionTitle("AI connection"));addGap(box,7);
        EditText base=field("API base URL",prefs.getString("base",""));EditText key=field("API key",prefs.getString("key",""));key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText model=field("Model",prefs.getString("model","nv-0.2"));
        box.addView(fieldCard("API base URL","The app sends OpenAI-compatible requests here.",base));box.addView(fieldCard("API key","Optional bearer key. Stored locally on this device.",key));box.addView(fieldCard("Model", "Model identifier used by your API.",model));
        addGap(box,18);box.addView(sectionTitle("Generation"));addGap(box,7);
        EditText temp=field("Temperature",prefs.getString("temperature","0.7"));EditText max=field("Max tokens",prefs.getString("max_tokens","1024"));
        box.addView(fieldCard("Temperature","0 = focused • 2 = more varied",temp));box.addView(fieldCard("Max tokens","Maximum response length.",max));
        addGap(box,18);box.addView(sectionTitle("Memory & behavior"));addGap(box,7);
        EditText mem=field("Local memory / system instruction",prefs.getString("memory",""));mem.setMinLines(5);
        box.addView(fieldCard("Local memory","Optional instruction included with each request.",mem));
        addGap(box,18);box.addView(sectionTitle("Privacy"));addGap(box,7);
        box.addView(card("Local conversation history","Chat history, title and settings are stored in the app's local preferences.","Network requests only go to the API endpoint you configure."));
        addGap(box,18);
        Button signout=btn("Sign out",false);
        signout.setOnClickListener(v->{prefs.edit().putBoolean("auth",false).apply();showAuth(false);});
        box.addView(signout,new LinearLayout.LayoutParams(-1,54));addGap(box,10);
        Button save=btn("Save settings",true);save.setOnClickListener(v->{prefs.edit().putString("base",base.getText().toString().trim().replaceAll("/$","")).putString("key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).putString("temperature",validTemp(temp.getText().toString())).putString("max_tokens",validMax(max.getText().toString())).putString("memory",mem.getText().toString()).apply();statusToast("Settings saved");});
        box.addView(save,new LinearLayout.LayoutParams(-1,54));addGap(box,12);
        Button transfer=btn("Import / Export conversations",false);transfer.setOnClickListener(v->showScreen("transfer"));box.addView(transfer,new LinearLayout.LayoutParams(-1,54));
        addGap(box,10);Button about=btn("About NOTVISIBLEAI",false);about.setOnClickListener(v->showScreen("about"));box.addView(about,new LinearLayout.LayoutParams(-1,54));
        sv.addView(box);content.addView(sv,new LinearLayout.LayoutParams(-1,-1));
    }
    private TextView sectionTitle(String s){TextView t=tv(s.toUpperCase(Locale.US),12,BLUE,true);return t;}
    private LinearLayout fieldCard(String title,String desc,EditText field){
        LinearLayout c=col();c.setBackground(outline(Color.WHITE,BORDER,18));pad(c,14,10,14,10);c.addView(tv(title,14,INK,true));c.addView(tv(desc,12,MUTED,false));addGap(c,3);field.setTextSize(14);field.setSingleLine(false);field.setBackground(outline(Color.WHITE,BORDER,12));field.setPadding(dp(12),dp(4),dp(12),dp(4));c.addView(field,new LinearLayout.LayoutParams(-1,dp(50)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));return wrap(c,p);
    }
    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(INK);return e;}

    private void renderTransfer(){
        ScrollView sv=new ScrollView(this);LinearLayout box=col();pad(box,16,16,16,24);
        box.addView(tv("Move your conversations",22,INK,true));box.addView(tv("Export a JSON backup or import one from this device.",14,MUTED,false));addGap(box,16);
        LinearLayout ex=card("Export chat","Save the current conversation as a JSON file.","Portable and easy to back up.");Button eb=btn("Export current chat",true);eb.setOnClickListener(v->exportChat());ex.addView(eb,new LinearLayout.LayoutParams(-1,50));box.addView(ex);
        addGap(box,12);LinearLayout im=card("Import chat","Load a previously exported conversation into this app.","The imported file replaces the current conversation.");Button ib=btn("Choose JSON file",false);ib.setOnClickListener(v->importChat());im.addView(ib,new LinearLayout.LayoutParams(-1,50));box.addView(im);
        addGap(box,20);box.addView(tv("Recent backup files",17,INK,true));addGap(box,8);box.addView(emptyCard("No cloud backup","NOTVISIBLEAI does not upload your conversations for backup."));
        sv.addView(box);content.addView(sv,new LinearLayout.LayoutParams(-1,-1));
    }

    private void renderAbout(){
        ScrollView sv=new ScrollView(this);LinearLayout box=col();pad(box,20,18,20,28);
        TextView n=tv("N",38,Color.WHITE,true);n.setGravity(Gravity.CENTER);n.setBackground(bg(INK,22));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(dp(78),dp(78));np.gravity=Gravity.CENTER_HORIZONTAL;box.addView(n,np);
        addGap(box,14);TextView h=tv("NOTVISIBLEAI",25,INK,true);h.setGravity(Gravity.CENTER);box.addView(h,new LinearLayout.LayoutParams(-1,38));
        TextView v=tv("v1.0.0 • NV-0.2 workspace",13,BLUE,false);v.setGravity(Gravity.CENTER);box.addView(v,new LinearLayout.LayoutParams(-1,28));addGap(box,18);
        box.addView(card("What it is","A private AI workspace designed to connect to your own compatible API and model infrastructure.","The Android client does not require ChatGPT or OpenAI to run its interface."));
        addGap(box,14);box.addView(card("What you can do","Chat, preserve local conversations, attach text context, use voice input, regenerate responses, and configure generation settings.","Your chosen API determines the actual model intelligence."));
        addGap(box,14);box.addView(card("Open model path","NOTVISIBLEAI is being developed toward independently owned model weights and inference infrastructure.","NV-0.2 is an experimental fine-tuned adapter; it is not a frontier-equivalent foundation model."));
        addGap(box,20);TextView links=tv("Privacy & data\n\n• Conversations are stored locally unless you send them through your configured API.\n• API keys are stored locally for this prototype.\n• Voice recognition uses Android's speech services when available.",14,MUTED,false);links.setBackground(outline(Color.WHITE,BORDER,18));pad(links,16,15,16,15);box.addView(links);
        sv.addView(box);content.addView(sv,new LinearLayout.LayoutParams(-1,-1));
    }

    private void renderError(){
        LinearLayout box=col();box.setGravity(Gravity.CENTER);pad(box,28,30,28,30);
        TextView i=tv("!",34,Color.WHITE,true);i.setGravity(Gravity.CENTER);i.setBackground(bg(Color.rgb(220,38,38),25));box.addView(i,new LinearLayout.LayoutParams(dp(68),dp(68)));
        addGap(box,18);TextView h=tv("Something went wrong",22,INK,true);h.setGravity(Gravity.CENTER);box.addView(h,new LinearLayout.LayoutParams(-1,38));
        TextView d=tv(prefs.getString("last_error","The API could not be reached."),14,MUTED,false);d.setGravity(Gravity.CENTER);box.addView(d,new LinearLayout.LayoutParams(-1,72));
        Button retry=btn("Retry",true);retry.setOnClickListener(v->{showScreen("chat");sendConversation(false);});box.addView(retry,new LinearLayout.LayoutParams(-1,54));
        addGap(box,10);Button settings=btn("Check API settings",false);settings.setOnClickListener(v->showScreen("settings"));box.addView(settings,new LinearLayout.LayoutParams(-1,54));
        content.addView(box,new LinearLayout.LayoutParams(-1,-1));
    }

    private void showMenu(){
        String[] items={"New chat","Rename chat","Clear conversation","Copy last response","Regenerate last response","Import / Export","About NOTVISIBLEAI"};
        new AlertDialog.Builder(this).setTitle("NOTVISIBLEAI").setItems(items,(d,w)->{
            if(w==0)newChat();else if(w==1)renameChat();else if(w==2)clearChat();else if(w==3)copyLast();else if(w==4)regenerate();else if(w==5)showScreen("transfer");else showScreen("about");
        }).show();
    }
    private String chatTitle(){return prefs.getString("title","New chat");}
    private void newChat(){history=new JSONArray();pendingFileContext="";prefs.edit().putString("history","").putString("title","New chat").apply();showScreen("chat");}
    private void clearChat(){new AlertDialog.Builder(this).setTitle("Clear conversation?").setMessage("This removes the current local conversation. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->newChat()).show();}
    private void renameChat(){
        EditText e=field("Chat name",chatTitle());e.setSingleLine();new AlertDialog.Builder(this).setTitle("Rename chat").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String s=e.getText().toString().trim();if(s.isEmpty())s="New chat";prefs.edit().putString("title",s).apply();showScreen("chat");}).show();
    }
    private void copyText(String s){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("NOTVISIBLEAI",s));statusToast("Copied to clipboard");}
    private void copyLast(){for(int i=history.length()-1;i>=0;i--)try{JSONObject m=history.getJSONObject(i);if(m.getString("role").equals("assistant")){copyText(m.getString("content"));return;}}catch(Exception ignored){}statusToast("No response to copy");}
    private void regenerate(){
        if(busy)return;try{int u=-1;for(int i=history.length()-1;i>=0;i--)if(history.getJSONObject(i).getString("role").equals("user")){u=i;break;}if(u<0){statusToast("No user message");return;}while(history.length()>u+1)history.remove(history.length()-1);saveHistory();showScreen("chat");sendConversation(false);}catch(Exception e){statusToast("Could not regenerate");}
    }

    private void sendMessage(){
        if(input==null)return;String q=input.getText().toString().trim();if(q.isEmpty()||busy)return;
        if(prefs.getString("base","").isEmpty()){showScreen("settings");return;}
        if(!pendingFileContext.isEmpty())q+="\n\n[Attached text]\n"+pendingFileContext;
        append("user",q);input.setText("");pendingFileContext="";showScreen("chat");sendConversation(false);
    }
    private void sendConversation(boolean ignored){
        String base=prefs.getString("base","");if(base.isEmpty()){showScreen("settings");return;}busy=true;
        final String model=prefs.getString("model","nv-0.2");final double temp=Double.parseDouble(prefs.getString("temperature","0.7"));final int max=Integer.parseInt(prefs.getString("max_tokens","1024"));final String memory=prefs.getString("memory","");
        statusToast("Thinking…");
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                JSONObject payload=new JSONObject();payload.put("model",model);payload.put("temperature",temp);payload.put("max_tokens",max);JSONArray msgs=new JSONArray();
                if(!memory.trim().isEmpty()){JSONObject sys=new JSONObject();sys.put("role","system");sys.put("content",memory);msgs.put(sys);}
                int start=Math.max(0,history.length()-40);for(int i=start;i<history.length();i++)msgs.put(history.getJSONObject(i));payload.put("messages",msgs);
                HttpURLConnection c=(HttpURLConnection)new URL(base+"/v1/chat/completions").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(180000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
                String key=prefs.getString("key","");if(!key.isEmpty())c.setRequestProperty("Authorization","Bearer "+key);
                OutputStream os=c.getOutputStream();os.write(payload.toString().getBytes(StandardCharsets.UTF_8));os.close();int code=c.getResponseCode();
                InputStream is=code>=400?c.getErrorStream():c.getInputStream();BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();String body=sb.toString();
                if(code>=400)throw new IOException("HTTP "+code);
                String answer=new JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                runOnUiThread(()->{append("assistant",answer);busy=false;showScreen("chat");});
            }catch(Exception e){String msg=e.getMessage()==null?"Unknown API error":e.getMessage();prefs.edit().putString("last_error",msg).apply();runOnUiThread(()->{busy=false;showScreen("error");});}
        });
    }
    private void append(String role,String text){try{JSONObject m=new JSONObject();m.put("role",role);m.put("content",text);history.put(m);saveHistory();}catch(Exception ignored){}}
    private void saveHistory(){prefs.edit().putString("history",history.toString()).apply();}
    private void loadHistory(){try{String s=prefs.getString("history","");if(!s.isEmpty())history=new JSONArray(s);}catch(Exception e){history=new JSONArray();}}

    private void pickFile(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_FILE);
    }
    private void exportChat(){try{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,chatTitle().replaceAll("[^a-zA-Z0-9._-]","_")+".json");startActivityForResult(i,CREATE_EXPORT);}catch(Exception e){statusToast("Export unavailable");}}
    private void importChat(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMPORT);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;Uri u=data.getData();if(u==null)return;
        try{
            if(request==PICK_IMPORT){String s=readUri(u);history=new JSONArray(s);saveHistory();showScreen("chat");statusToast("Chat imported");}
            else if(request==CREATE_EXPORT){OutputStream os=getContentResolver().openOutputStream(u);os.write(history.toString(2).getBytes(StandardCharsets.UTF_8));os.close();statusToast("Chat exported");}
            else if(request==PICK_FILE){String s=readUri(u);if(s.length()>30000)s=s.substring(0,30000);pendingFileContext=s;String name=u.getLastPathSegment();saveFileName(name==null?"Text file":name);showScreen("files");statusToast("File attached to next message");}
        }catch(Exception e){statusToast("File operation failed");}
    }
    private String readUri(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();String l;while((l=br.readLine())!=null)b.append(l).append('\n');br.close();return b.toString();}
    private void saveFileName(String n){String old=prefs.getString("files","");if(!old.contains(n)){String v=old.isEmpty()?n:old+"|"+n;prefs.edit().putString("files",v).apply();}}

    private boolean speechAvailable(){return SpeechRecognizer.isRecognitionAvailable(this);}
    private void setupSpeech(){
        speech=SpeechRecognizer.createSpeechRecognizer(this);speech.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){}public void onBeginningOfSpeech(){statusToast("Listening…");}public void onRmsChanged(float x){}public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){statusToast("Processing voice…");}public void onError(int e){statusToast("Voice input unavailable");}
            public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){if(input!=null){input.setText(r.get(0));input.setSelection(input.length());}showScreen("chat");statusToast("Voice added");}}
            public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}
        });
    }
    private void startVoice(){
        if(speech==null){statusToast("Speech recognition unavailable");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;}
        showVoiceOverlay();Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);speech.startListening(i);
    }
    private void showVoiceOverlay(){
        final Dialog d=new Dialog(this);LinearLayout b=col();b.setGravity(Gravity.CENTER);pad(b,26,28,26,28);b.setBackground(bg(Color.WHITE,28));
        TextView icon=tv("◉",48,Color.WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(bg(BLUE,40));b.addView(icon,new LinearLayout.LayoutParams(dp(100),dp(100)));
        addGap(b,18);TextView h=tv("Listening…",24,INK,true);h.setGravity(Gravity.CENTER);b.addView(h,new LinearLayout.LayoutParams(-1,40));
        TextView sub=tv("Speak naturally. Your words will appear in the message box.",14,MUTED,false);sub.setGravity(Gravity.CENTER);b.addView(sub,new LinearLayout.LayoutParams(-1,60));
        Button stop=btn("Stop listening",true);stop.setOnClickListener(v->{if(speech!=null)speech.stopListening();d.dismiss();});b.addView(stop,new LinearLayout.LayoutParams(-1,54));
        d.setContentView(b);Window w=d.getWindow();if(w!=null)w.setBackgroundDrawableResource(android.R.color.transparent);d.show();if(w!=null)w.setLayout(dp(330),WindowManager.LayoutParams.WRAP_CONTENT);
    }
    private void statusToast(String s){if(status!=null)status.setText(s);Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private String validTemp(String raw){
        try{
            double v=Double.parseDouble(raw.trim());
            if(Double.isNaN(v)||Double.isInfinite(v)) return "0.7";
            v=Math.max(0.0,Math.min(2.0,v));
            return String.valueOf(v);
        }catch(Exception e){return "0.7";}
    }
    private String validMax(String raw){
        try{
            int v=Integer.parseInt(raw.trim());
            v=Math.max(64,Math.min(32768,v));
            return String.valueOf(v);
        }catch(Exception e){return "1024";}
    }

    @Override public void onBackPressed(){
        if(screen.equals("settings")||screen.equals("files")||screen.equals("about")||screen.equals("transfer")||screen.equals("error")){showScreen("chat");return;}
        super.onBackPressed();
    }
}
