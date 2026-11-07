package com.gamebox.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import androidx.webkit.WebViewAssetLoader;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity {
  private static final int BG = Color.rgb(7, 22, 15);
  private static final int PANEL = Color.rgb(14, 39, 27);
  private static final int GREEN = Color.rgb(24, 91, 57);
  private static final int LIGHT = Color.rgb(195, 225, 205);
  private final Random random = new Random();
  private final Handler handler = new Handler();
  private android.content.SharedPreferences prefs;
  private ToneGenerator tone;
  private Game currentGame;
  private LinearLayout page;
  private int gamesPlayed = 0;
  private WebView puzzleWebView;
  private FrameLayout puzzleFrame;
  private Button puzzleFullscreenButton;
  private String embeddedOrientationKey;

  private final Game[] games = new Game[] {
    new Game("memory", "זיכרון קלפים", "זיכרון", "מצא את כל זוגות הקלפים"),
    new Game("xo", "איקס עיגול", "אסטרטגיה", "שני שחקנים על אותו מכשיר"),
    new Game("2048", "2048", "חשיבה", "מזג מספרים והגע ל-2048"),
    new Game("tap", "אתגר מהירות", "ארקייד", "כמה מהר תוכל ללחוץ 20 פעמים?"),
    new Game("race", "מרוץ תגובה", "מירוצים", "הזנק את המכונית רק כשהרמזור ירוק"),
    new Game("math", "אתגר חשבון", "חשיבה", "פתור חמש שאלות ברצף"),
    new Game("rps", "אבן נייר ומספריים", "קלפים", "שחק מול המחשב"),
    new Game("guess", "נחש את המספר", "הרפתקאות", "מצא מספר סודי בין 1 ל-100"),
    new Game("words", "אתגר מילים", "מילים", "בחר את המילה המתאימה"),
    new Game("lights", "כיבוי אורות", "אסטרטגיה", "כבה את כל האורות בלוח"),
    new Game("penalty", "דו-קרב פנדלים", "ספורט", "בעט לפינות ונסה להבקיע"),
    new Game("connect", "ארבע בשורה", "אסטרטגיה", "שני שחקנים — חבר ארבעה סמלים ברצף"),
    new Game("master", "קוד סודי", "חשיבה", "גלה את רצף ארבע הספרות"),
    new Game("slide", "פאזל הזזה", "הרפתקאות", "סדר את המספרים לפי הסדר")
  };

  // Shared game state
  private int[] memoryCards;
  private boolean[] memoryShown, memoryMatched;
  private int memoryA = -1, memoryB = -1, memoryPairs = 0;
  private boolean memoryBusy = false;
  private int[] xoBoard;
  private int xoTurn = 1;
  private boolean xoDone = false;
  private int[][] board2048;
  private int score2048 = 0;
  private int secret, guessCount;
  private int mathA, mathB, mathAnswer, mathCorrect;
  private long tapStart;
  private int tapCount;
  private int wordIndex, wordScore;
  private boolean[][] lights;
  private int penaltyRound, penaltyGoals;
  private long lastTapTime;
  private long raceReadyAt;
  private boolean racePending, raceSignal, raceFalseStart;
  private int[] connectBoard;
  private int connectTurn = 1;
  private boolean connectDone;
  private int[] masterSecret;
  private int masterTries;
  private String masterHint = "נחש רצף של ארבע ספרות שונות בין 1 ל-6";
  private int[] slideBoard;
  private int slideMoves;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    prefs = getSharedPreferences("gamebox", MODE_PRIVATE);
    gamesPlayed = prefs.getInt("games_played", 0);
    try { tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45); } catch (Exception ignored) {}
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    showHome();
  }

  private void showHome() {
    currentGame = null;
    applySavedFullscreen();
    page = newPage();
    addHeader(page, "GAME BOX", "ארגז המשחקים שלך");
    TextView stats = text("שיחקת " + gamesPlayed + " פעמים  •  התקדמות נשמרת במכשיר", 13, LIGHT);
    stats.setGravity(Gravity.CENTER);
    page.addView(stats, matchWrap());
    addGap(page, 18);
    addButton(page, "🎮  בחירת משחקים", v -> showCatalog("הכול"), true);
    addButton(page, "🧩  304 משחקי חשיבה ופאזלים", v -> openPuzzleSuite(), true);
    addButton(page, "🎲  190 משחקים נוספים", v -> openMiniGames(), true);
    addButton(page, "⚙  הגדרות", v -> showSettings(), false);
    addButton(page, "★  המשחקים והשלבים שלי", v -> showProgress(), false);
    addGap(page, 12);
    TextView note = text("14 משחקי בסיס + 304 פאזלים + 190 משחקי HTML5 • הכול אופליין", 13, Color.LTGRAY);
    note.setGravity(Gravity.CENTER);
    page.addView(note, matchWrap());
    setContentView(wrap(page));
  }

  private void showCatalog(String category) {
    currentGame = null;
    page = newPage();
    addHeader(page, "בחירת משחק", "בחר קטגוריה ואז משחק");
    addButton(page, "כל המשחקים", v -> showCatalog("הכול"), category.equals("הכול"));
    String[] cats = {"חשיבה","אסטרטגיה","ארקייד","מירוצים","קלפים","ספורט","הרפתקאות","זיכרון","מילים"};
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER);
    for (String c : cats) {
      Button b = smallButton(c);
      b.setOnClickListener(v -> showCatalog(c));
      row.addView(b, new LinearLayout.LayoutParams(0, 48, 1));
    }
    page.addView(row, matchWrap());
    addGap(page, 10);
    int count = 0;
    for (Game g : games) {
      if (!category.equals("הכול") && !g.category.equals(category)) continue;
      count++;
      int stage = prefs.getInt("stage_" + g.id, 1);
      addButton(page, g.title + "  ·  " + g.category + "\n" + g.description + "\nשלב " + stage + " מתוך 50", v -> launch(g), false);
    }
    if (count == 0) {
      TextView empty = text("בקטגוריה הזו עדיין אין משחק. בחר קטגוריה אחרת.", 16, LIGHT);
      page.addView(empty, matchWrap());
    }
    addButton(page, "חזרה", v -> showHome(), false);
    setContentView(wrap(page));
  }

  private void launch(Game g) {
    currentGame = g;
    applyGameOrientation(g.id);
    if (g.id.equals("memory")) initMemory();
    if (g.id.equals("xo")) { xoBoard = new int[9]; xoTurn = 1; xoDone = false; }
    if (g.id.equals("2048")) init2048();
    if (g.id.equals("guess")) { secret = random.nextInt(100) + 1; guessCount = 0; }
    if (g.id.equals("math")) { mathCorrect = 0; newMath(); }
    if (g.id.equals("tap")) { tapCount = 0; tapStart = 0; lastTapTime = 0; }
    if (g.id.equals("race")) { raceReadyAt = 0; racePending = false; raceSignal = false; raceFalseStart = false; }
    if (g.id.equals("connect")) { connectBoard = new int[42]; connectTurn = 1; connectDone = false; }
    if (g.id.equals("master")) initMaster();
    if (g.id.equals("slide")) initSlide();
    if (g.id.equals("words")) { wordScore = 0; wordIndex = random.nextInt(WORD_Q.length); }
    if (g.id.equals("lights")) initLights();
    if (g.id.equals("penalty")) { penaltyRound = 0; penaltyGoals = 0; }
    renderGame();
  }

  private void renderGame() {
    if (currentGame == null) return;
    page = newPage();
    int stage = prefs.getInt("stage_" + currentGame.id, 1);
    addHeader(page, currentGame.title, "שלב " + stage + " מתוך 50");
    addButton(page, "⛶  מסך מלא / יציאה ממסך מלא", v -> toggleFullscreen(), false);
    addOrientationControls(page, currentGame.id);
    switch (currentGame.id) {
      case "memory": renderMemory(); break;
      case "xo": renderXO(); break;
      case "2048": render2048(); break;
      case "tap": renderTap(); break;
      case "race": renderRace(); break;
      case "math": renderMath(); break;
      case "rps": renderRps(); break;
      case "guess": renderGuess(); break;
      case "words": renderWords(); break;
      case "lights": renderLights(); break;
      case "penalty": renderPenalty(); break;
      case "connect": renderConnect(); break;
      case "master": renderMaster(); break;
      case "slide": renderSlide(); break;
    }
    addGap(page, 16);
    addButton(page, "↻  התחל משחק מחדש", v -> launch(currentGame), false);
    addButton(page, "חזרה לרשימת המשחקים", v -> showCatalog("הכול"), false);
    addButton(page, "מסך הבית", v -> showHome(), false);
    setContentView(wrap(page));
  }

  private void renderMemory() {
    TextView info = text("מצא את " + (memoryCards.length / 2) + " הזוגות", 18, LIGHT);
    info.setGravity(Gravity.CENTER); page.addView(info, matchWrap());
    for (int r=0; r<4; r++) {
      LinearLayout row = new LinearLayout(this); row.setOrientation(0);
      for (int c=0; c<4; c++) {
        final int i = r*4+c;
        String label = memoryMatched[i] ? "✓" : (memoryShown[i] ? String.valueOf(memoryCards[i]) : "❖");
        Button b = smallButton(label);
        b.setTextSize(23);
        b.setEnabled(!memoryMatched[i] && !memoryBusy && !memoryShown[i]);
        b.setOnClickListener(v -> memoryFlip(i));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 66, 1);
        p.setMargins(3,3,3,3); row.addView(b,p);
      }
      page.addView(row, matchWrap());
    }
    TextView score = text("זוגות שנמצאו: " + memoryPairs + " / 8", 15, LIGHT);
    score.setGravity(Gravity.CENTER); page.addView(score, matchWrap());
  }

  private void memoryFlip(int i) {
    if (memoryBusy || memoryShown[i] || memoryMatched[i]) return;
    sound();
    memoryShown[i] = true;
    if (memoryA < 0) memoryA = i;
    else {
      memoryB = i;
      if (memoryCards[memoryA] == memoryCards[memoryB]) {
        memoryMatched[memoryA] = true; memoryMatched[memoryB] = true;
        memoryPairs++; memoryA = -1; memoryB = -1;
        if (memoryPairs == 8) { renderGame(); finishStage("כל הזוגות נמצאו!"); return; }
      } else {
        memoryBusy = true;
        renderGame();
        handler.postDelayed(() -> {
          memoryShown[memoryA] = false; memoryShown[memoryB] = false;
          memoryA = -1; memoryB = -1; memoryBusy = false; renderGame();
        }, 850);
        return;
      }
    }
    renderGame();
  }

  private void renderXO() {
    TextView info = text(xoDone ? "הסיבוב הסתיים" : (xoTurn == 1 ? "תור X" : "תור O"), 20, LIGHT);
    info.setGravity(Gravity.CENTER); page.addView(info, matchWrap());
    for (int r=0;r<3;r++) {
      LinearLayout row = new LinearLayout(this);
      for (int c=0;c<3;c++) {
        final int i=r*3+c;
        Button b=smallButton(xoBoard[i]==1?"X":(xoBoard[i]==2?"O":" "));
        b.setTextSize(27); b.setEnabled(!xoDone && xoBoard[i]==0);
        b.setOnClickListener(v -> {
          if (xoDone || xoBoard[i]!=0) return;
          xoBoard[i]=xoTurn; sound();
          int winner=xoWinner();
          if (winner!=0) { xoDone=true; renderGame(); finishStage(winner==1?"X ניצח!":"O ניצח!"); }
          else if (xoFull()) { xoDone=true; renderGame(); toast("תיקו!"); }
          else { xoTurn=3-xoTurn; renderGame(); }
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,72,1); p.setMargins(3,3,3,3); row.addView(b,p);
      }
      page.addView(row,matchWrap());
    }
    addButton(page,"סיבוב חדש",v->{xoBoard=new int[9];xoTurn=1;xoDone=false;renderGame();},false);
  }


  private void renderConnect() {
    TextView info=text(connectDone?"הסיבוב הסתיים":(connectTurn==1?"תור השחקן האדום":"תור השחקן הירוק"),18,LIGHT);
    info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    for(int r=0;r<6;r++){
      LinearLayout row=new LinearLayout(this);
      for(int col=0;col<7;col++){
        final int cc=col;int value=connectBoard[r*7+col];
        Button b=smallButton(value==1?"🔴":(value==2?"🟢":"·"));b.setTextSize(18);
        b.setEnabled(!connectDone);
        b.setOnClickListener(v->dropConnect(cc));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,45,1);p.setMargins(1,2,1,2);row.addView(b,p);
      }
      page.addView(row,matchWrap());
    }
    addButton(page,"משחק חדש",v->{connectBoard=new int[42];connectTurn=1;connectDone=false;renderGame();},false);
  }

  private void dropConnect(int col) {
    if(connectDone)return;
    for(int row=5;row>=0;row--){
      int index=row*7+col;
      if(connectBoard[index]==0){
        connectBoard[index]=connectTurn;sound();
        if(connectHasWon(connectTurn)){connectDone=true;renderGame();finishStage(connectTurn==1?"השחקן האדום ניצח!":"השחקן הירוק ניצח!");return;}
        boolean full=true;for(int x:connectBoard)if(x==0){full=false;break;}
        if(full){connectDone=true;renderGame();toast("תיקו!");return;}
        connectTurn=3-connectTurn;renderGame();return;
      }
    }
  }

  private boolean connectHasWon(int player) {
    int[][] dirs={{0,1},{1,0},{1,1},{1,-1}};
    for(int r=0;r<6;r++)for(int col=0;col<7;col++)if(connectBoard[r*7+col]==player){
      for(int[] d:dirs){int count=1;for(int k=1;k<4;k++){int rr=r+d[0]*k,cc=col+d[1]*k;if(rr<0||rr>=6||cc<0||cc>=7||connectBoard[rr*7+cc]!=player)break;count++;}if(count==4)return true;}
    }
    return false;
  }

  private void initMaster() {
    masterSecret=new int[4];List<Integer> digits=new ArrayList<>();for(int i=1;i<=6;i++)digits.add(i);Collections.shuffle(digits,random);
    for(int i=0;i<4;i++)masterSecret[i]=digits.get(i);
    masterTries=0;masterHint="נחש רצף של ארבע ספרות שונות בין 1 ל-6";
  }

  private void renderMaster() {
    TextView info=text(masterHint,17,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    TextView tries=text("ניסיונות: "+masterTries+" / 8",15,LIGHT);tries.setGravity(Gravity.CENTER);page.addView(tries,matchWrap());
    EditText input=edit("לדוגמה 1234");input.setInputType(InputType.TYPE_CLASS_NUMBER);page.addView(input,matchWrap());
    addButton(page,"בדוק קוד",v->{
      String value=input.getText().toString().trim();
      if(value.length()!=4){toast("צריך להכניס ארבע ספרות");return;}
      boolean[] seen=new boolean[7];int exact=0,close=0;
      for(int i=0;i<4;i++){int n=value.charAt(i)-'0';if(n<1||n>6||seen[n]){toast("השתמש בספרות שונות מ-1 עד 6");return;}seen[n]=true;if(n==masterSecret[i])exact++;else for(int j=0;j<4;j++)if(n==masterSecret[j])close++;}
      if(exact==4){renderGame();finishStage("פיצחת את הקוד הסודי!");return;}
      masterTries++;masterHint="במקום הנכון: "+exact+"  •  קיימות אך במקום אחר: "+close;
      if(masterTries>=8){String answer="הקוד היה "+masterSecret[0]+masterSecret[1]+masterSecret[2]+masterSecret[3];initMaster();masterHint="לא הפעם. "+answer+" — נסה קוד חדש.";renderGame();return;}
      renderGame();
    },true);
  }

  private void initSlide() {
    slideBoard=new int[]{1,2,3,4,5,6,7,8,0};slideMoves=0;int previous=-1;
    for(int n=0;n<70;n++){int zero=0;for(int i=0;i<9;i++)if(slideBoard[i]==0)zero=i;List<Integer> choices=new ArrayList<>();int r=zero/3,col=zero%3;if(r>0&&zero-3!=previous)choices.add(zero-3);if(r<2&&zero+3!=previous)choices.add(zero+3);if(col>0&&zero-1!=previous)choices.add(zero-1);if(col<2&&zero+1!=previous)choices.add(zero+1);int pick=choices.get(random.nextInt(choices.size()));slideBoard[zero]=slideBoard[pick];slideBoard[pick]=0;previous=zero;}
  }

  private void renderSlide() {
    TextView info=text("הזז את האריחים למקומם. מהלכים: "+slideMoves,16,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    for(int r=0;r<3;r++){
      LinearLayout row=new LinearLayout(this);
      for(int col=0;col<3;col++){final int i=r*3+col;Button b=smallButton(slideBoard[i]==0?" ":String.valueOf(slideBoard[i]));b.setTextSize(24);b.setEnabled(slideBoard[i]!=0);b.setOnClickListener(v->slideMove(i));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,76,1);p.setMargins(3,3,3,3);row.addView(b,p);}
      page.addView(row,matchWrap());
    }
  }

  private void slideMove(int index) {
    int zero=0;for(int i=0;i<9;i++)if(slideBoard[i]==0)zero=i;
    int r=index/3,c=index%3,zr=zero/3,zc=zero%3;
    if(Math.abs(r-zr)+Math.abs(c-zc)!=1)return;
    slideBoard[zero]=slideBoard[index];slideBoard[index]=0;slideMoves++;sound();renderGame();
    boolean solved=true;for(int i=0;i<8;i++)if(slideBoard[i]!=i+1)solved=false;
    if(solved)finishStage("השלמת את הפאזל ב-"+slideMoves+" מהלכים!");
  }

  private void render2048() {
    TextView s=text("ניקוד: "+score2048+"   •   חבר אריחים עד 2048",16,LIGHT);
    s.setGravity(Gravity.CENTER); page.addView(s,matchWrap());
    for(int r=0;r<4;r++) {
      LinearLayout row=new LinearLayout(this);
      for(int c=0;c<4;c++) {
        int value=board2048[r][c];
        Button b=smallButton(value==0?"":String.valueOf(value)); b.setTextSize(19);
        b.setEnabled(false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,63,1);p.setMargins(3,3,3,3);row.addView(b,p);
      }
      page.addView(row,matchWrap());
    }
    LinearLayout dirs=new LinearLayout(this);
    String[] labels={"←","→","↑","↓"};
    for(int i=0;i<4;i++){final int d=i;Button b=smallButton(labels[i]);b.setTextSize(22);b.setOnClickListener(v->move2048(d));dirs.addView(b,new LinearLayout.LayoutParams(0,54,1));}
    page.addView(dirs,matchWrap());
  }

  private void move2048(int dir) {
    boolean changed=false;
    for(int line=0;line<4;line++) {
      int[] a=new int[4]; int n=0;
      for(int k=0;k<4;k++){int r,c;if(dir==0||dir==1){r=line;c=(dir==0?k:3-k);}else{r=(dir==2?k:3-k);c=line;}if(board2048[r][c]!=0)a[n++]=board2048[r][c];}
      for(int k=0;k<n-1;k++){if(a[k]==a[k+1]){a[k]*=2;score2048+=a[k];for(int j=k+1;j<n-1;j++)a[j]=a[j+1];a[--n]=0;}}
      for(int k=0;k<4;k++){int r,c;if(dir==0||dir==1){r=line;c=(dir==0?k:3-k);}else{r=(dir==2?k:3-k);c=line;}if(board2048[r][c]!=a[k])changed=true;board2048[r][c]=a[k];}
    }
    if(changed) add2048Tile();
    renderGame();
    if(has2048()) finishStage("הגעת ל-2048! כל הכבוד.");
  }

  private void renderTap() {
    TextView info=text("לחיצות: "+tapCount+" / 20",22,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    Button b=largeButton(tapCount==0?"התחל!":(tapCount<20?"לחץ כאן במהירות!":"הושלם"));
    b.setOnClickListener(v->{
      if(tapCount>=20)return;
      if(tapCount==0)tapStart=android.os.SystemClock.elapsedRealtime();
      tapCount++;sound();
      if(tapCount==20){long ms=android.os.SystemClock.elapsedRealtime()-tapStart;renderGame();finishStage("סיימת 20 לחיצות ב-"+(ms/1000.0)+" שניות!");}
      else renderGame();
    });
    page.addView(b,new LinearLayout.LayoutParams(-1,115));
  }


  private void renderRace() {
    TextView info = text(raceFalseStart ? "זינוק מוקדם! פסילה." :
      (raceSignal ? "🟢 אור ירוק! סע עכשיו!" :
      (racePending ? "🔴 אור אדום — המתן..." : "מוכן למרוץ? חכה לאור הירוק.")), 20, LIGHT);
    info.setGravity(Gravity.CENTER); page.addView(info, matchWrap());
    if (raceFalseStart) {
      addButton(page, "נסה שוב", v -> launch(currentGame), true);
    } else if (!racePending) {
      addButton(page, "התחל מרוץ", v -> {
        racePending = true; raceSignal = false;
        raceReadyAt = android.os.SystemClock.elapsedRealtime() + 1800 + random.nextInt(2800);
        renderGame();
        handler.postDelayed(() -> {
          if (currentGame != null && currentGame.id.equals("race") && racePending) {
            raceSignal = true; sound(); renderGame();
          }
        }, raceReadyAt - android.os.SystemClock.elapsedRealtime());
      }, true);
    } else if (!raceSignal) {
      addButton(page, "לחץ רק כשהאור ירוק!", v -> {
        raceFalseStart = true; racePending = false; renderGame();
      }, false);
    } else {
      addButton(page, "🏎  סע!", v -> {
        long ms = android.os.SystemClock.elapsedRealtime() - raceReadyAt;
        racePending = false; raceSignal = false;
        if (ms <= 850) finishStage("זמן תגובה מצוין: " + ms + " מילישניות!");
        else { renderGame(); toast("זמן תגובה: " + ms + " מילישניות. נסה להיות מהיר יותר."); }
      }, true);
    }
  }

  private void renderMath() {
    TextView q=text(mathA+"  +  "+mathB+"  =  ?",30,LIGHT);q.setGravity(Gravity.CENTER);page.addView(q,matchWrap());
    TextView progress=text("תשובות נכונות: "+mathCorrect+" / 5",16,LIGHT);progress.setGravity(Gravity.CENTER);page.addView(progress,matchWrap());
    EditText input=edit("התשובה שלך");
    page.addView(input,matchWrap());
    addButton(page,"בדוק תשובה",v->{
      String x=input.getText().toString().trim();
      if(x.isEmpty()){toast("כתוב תשובה");return;}
      if(parse(x,-999)==mathAnswer){mathCorrect++;sound();if(mathCorrect>=5){renderGame();finishStage("חמש תשובות נכונות!");}else{newMath();renderGame();}}
      else toast("לא מדויק. נסה שוב.");
    },true);
  }

  private void renderRps() {
    TextView info=text("בחר את המהלך שלך",20,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    String[] opts={"אבן","נייר","מספריים"};
    for(int i=0;i<3;i++){final int x=i;addButton(page,opts[i],v->{int cpu=random.nextInt(3);String result=x==cpu?"תיקו":((x-cpu+3)%3==1?"ניצחת!":"המחשב ניצח");Toast.makeText(this,"אתה: "+opts[x]+" • מחשב: "+opts[cpu]+"\n"+result,Toast.LENGTH_LONG).show();if(result.equals("ניצחת!"))finishStage("ניצחת את המחשב!");},false);}
  }

  private void renderGuess() {
    TextView info=text("מצא את המספר הסודי בין 1 ל-100",19,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    page.addView(text("מספר ניסיונות: "+guessCount,15,LIGHT),matchWrap());
    EditText input=edit("הכנס מספר");
    input.setInputType(InputType.TYPE_CLASS_NUMBER);page.addView(input,matchWrap());
    addButton(page,"בדוק",v->{int n=parse(input.getText().toString(),-1);if(n<1||n>100){toast("בחר מספר בין 1 ל-100");return;}guessCount++;if(n==secret){renderGame();finishStage("מצאת את המספר ב-"+guessCount+" ניסיונות!");}else toast(n<secret?"גדול יותר ↑":"קטן יותר ↓");},true);
  }

  private void renderWords() {
    TextView info=text("בחר את ההשלמה הנכונה",18,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    TextView q=text(WORD_Q[wordIndex][0],25,Color.WHITE);q.setGravity(Gravity.CENTER);page.addView(q,matchWrap());
    String[] opts={WORD_Q[wordIndex][1],WORD_Q[wordIndex][2],WORD_Q[wordIndex][3]};
    List<String> shuffled=new ArrayList<>();Collections.addAll(shuffled,opts);Collections.shuffle(shuffled,random);
    for(String option:shuffled)addButton(page,option,v->{if(option.equals(WORD_Q[wordIndex][1])){wordScore++;if(wordScore>=5){renderGame();finishStage("חמש תשובות נכונות!");}else{wordIndex=(wordIndex+1)%WORD_Q.length;renderGame();}}else toast("נסה שוב!");},false);
    TextView s=text("תשובות נכונות: "+wordScore+" / 5",15,LIGHT);s.setGravity(Gravity.CENTER);page.addView(s,matchWrap());
  }

  private void renderLights() {
    TextView info=text("כבה את כל המשבצות. לחיצה משנה גם את השכנים.",15,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    for(int r=0;r<5;r++){
      LinearLayout row=new LinearLayout(this);
      for(int c=0;c<5;c++){final int rr=r,cc=c;Button b=smallButton(lights[r][c]?"●":"○");b.setTextSize(19);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(lights[r][c]?GREEN:PANEL));b.setOnClickListener(v->{toggleLight(rr,cc);renderGame();if(allLightsOff())finishStage("כיבית את כל האורות!");});row.addView(b,new LinearLayout.LayoutParams(0,52,1));}
      page.addView(row,matchWrap());
    }
  }

  private void renderPenalty() {
    TextView info=text("בעיטות: "+penaltyRound+" / 5    שערים: "+penaltyGoals,18,LIGHT);info.setGravity(Gravity.CENTER);page.addView(info,matchWrap());
    addButton(page,"בעיטה לפינה השמאלית",v->kick(0),false);
    addButton(page,"בעיטה למרכז",v->kick(1),false);
    addButton(page,"בעיטה לפינה הימנית",v->kick(2),false);
    if(penaltyRound>=5)addButton(page,"סיים דו-קרב",v->{if(penaltyGoals>=3)finishStage("ניצחת בדו-קרב!");else toast("נסה שוב כדי לשפר את התוצאה.");},true);
  }

  private void kick(int choice) {
    if(penaltyRound>=5)return;
    int keeper=random.nextInt(3);penaltyRound++;if(choice!=keeper)penaltyGoals++;
    toast(choice==keeper?"השוער עצר!":"גול! ⚽");
    renderGame();
    if(penaltyRound==5 && penaltyGoals>=3)finishStage("ניצחת בדו-קרב הפנדלים!");
  }


  private int dp(float value) {
    return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
  }


  private void openPuzzleSuite() {
    openEmbeddedLibrary("puzzle_suite", "puzzle/index.html");
  }

  private void openMiniGames() {
    openEmbeddedLibrary("mini_games", "mini/index.html");
  }

  private void openEmbeddedLibrary(String orientationKey, String assetPage) {
    currentGame = null;
    embeddedOrientationKey = orientationKey;
    applyGameOrientation(orientationKey);
    puzzleFrame = new FrameLayout(this);
    puzzleWebView = new WebView(this);
    WebSettings settings = puzzleWebView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setAllowFileAccess(false);
    settings.setAllowContentAccess(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    settings.setMediaPlaybackRequiresUserGesture(true);
    WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();
    puzzleWebView.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return loader.shouldInterceptRequest(request.getUrl());
      }
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String host = request.getUrl().getHost();
        return !"appassets.androidplatform.net".equals(host);
      }
    });
    FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    puzzleFrame.addView(puzzleWebView, webParams);

    LinearLayout toolbar = new LinearLayout(this);
    toolbar.setOrientation(LinearLayout.HORIZONTAL);
    toolbar.setGravity(Gravity.CENTER);
    toolbar.setPadding(dp(4), dp(4), dp(4), dp(4));
    toolbar.setBackgroundColor(Color.rgb(8, 25, 17));
    Button back = smallButton("חזרה");
    back.setOnClickListener(v -> closeEmbeddedLibrary());
    toolbar.addView(back, new LinearLayout.LayoutParams(0, dp(44), 1.1f));

    Button portrait = smallButton("לאורך");
    portrait.setOnClickListener(v -> setEmbeddedOrientation(orientationKey, "portrait"));
    toolbar.addView(portrait, new LinearLayout.LayoutParams(0, dp(44), 1));

    Button landscape = smallButton("לרוחב");
    landscape.setOnClickListener(v -> setEmbeddedOrientation(orientationKey, "landscape"));
    toolbar.addView(landscape, new LinearLayout.LayoutParams(0, dp(44), 1));

    puzzleFullscreenButton = smallButton("⛶ מסך מלא");
    puzzleFullscreenButton.setOnClickListener(v -> toggleEmbeddedFullscreen());
    toolbar.addView(puzzleFullscreenButton, new LinearLayout.LayoutParams(0, dp(44), 1.4f));

    FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT, dp(52), Gravity.TOP);
    puzzleFrame.addView(toolbar, barParams);
    FrameLayout.LayoutParams contentParams = (FrameLayout.LayoutParams)puzzleWebView.getLayoutParams();
    contentParams.topMargin = dp(52);
    puzzleWebView.setLayoutParams(contentParams);
    setContentView(puzzleFrame);
    puzzleWebView.loadUrl("https://appassets.androidplatform.net/assets/" + assetPage);
  }

  private void setEmbeddedOrientation(String key, String value) {
    prefs.edit().putString("orientation_" + key, value).apply();
    applyOrientation(value);
    toast("כיוון המסך נשמר");
  }

  private void setPuzzleOrientation(String value) {
    setEmbeddedOrientation("puzzle_suite", value);
  }

  private void toggleEmbeddedFullscreen() {
    boolean enabled = !prefs.getBoolean("fullscreen", false);
    prefs.edit().putBoolean("fullscreen", enabled).apply();
    applyFullscreen(enabled);
    if (puzzleFullscreenButton != null) {
      puzzleFullscreenButton.setText(enabled ? "⛶ יציאה" : "⛶ מסך מלא");
    }
  }

  private void closeEmbeddedLibrary() {
    applyFullscreen(false);
    prefs.edit().putBoolean("fullscreen", false).apply();
    if (puzzleWebView != null) {
      puzzleWebView.stopLoading();
      puzzleWebView.loadUrl("about:blank");
      puzzleWebView.removeAllViews();
      puzzleWebView.destroy();
      puzzleWebView = null;
    }
    puzzleFrame = null;
    puzzleFullscreenButton = null;
    embeddedOrientationKey = null;
    showHome();
  }

  private void showSettings() {
    page=newPage();addHeader(page,"הגדרות","התאם את חוויית המשחק");
    TextView orient=text("כיוון מסך כללי",18,LIGHT);page.addView(orient,matchWrap());
    addButton(page,"אוטומטי",v->setGlobalOrientation("system"),false);
    addButton(page,"לאורך",v->setGlobalOrientation("portrait"),false);
    addButton(page,"לרוחב",v->setGlobalOrientation("landscape"),false);
    addButton(page,prefs.getBoolean("fullscreen",false)?"כבה מסך מלא":"הפעל מסך מלא",v->toggleFullscreen(),false);
    addButton(page,prefs.getBoolean("sound",true)?"כבה צלילים":"הפעל צלילים",v->{prefs.edit().putBoolean("sound",!prefs.getBoolean("sound",true)).apply();showSettings();},false);
    addButton(page,"כיוון משחקי החשיבה – אוטומטי",v->setPuzzleOrientation("system"),false);
    addButton(page,"כיוון משחקי החשיבה – לאורך",v->setPuzzleOrientation("portrait"),false);
    addButton(page,"כיוון משחקי החשיבה – לרוחב",v->setPuzzleOrientation("landscape"),false);
    addButton(page,"כיוון 190 המשחקים הנוספים – אוטומטי",v->setEmbeddedOrientation("mini_games","system"),false);
    addButton(page,"כיוון 190 המשחקים הנוספים – לאורך",v->setEmbeddedOrientation("mini_games","portrait"),false);
    addButton(page,"כיוון 190 המשחקים הנוספים – לרוחב",v->setEmbeddedOrientation("mini_games","landscape"),false);
    addButton(page,"איפוס התקדמות",v->new android.app.AlertDialog.Builder(this).setTitle("איפוס נתונים").setMessage("למחוק את כל השלבים והסטטיסטיקה?").setNegativeButton("ביטול",null).setPositiveButton("אפס",(d,w)->{prefs.edit().clear().apply();gamesPlayed=0;showHome();}).show(),false);
    addButton(page,"חזרה",v->showHome(),false);
    setContentView(wrap(page));
  }

  private void showProgress() {
    page=newPage();addHeader(page,"ההתקדמות שלי","השלבים נשמרים במכשיר");
    for(Game g:games){int stage=prefs.getInt("stage_"+g.id,1);addButton(page,g.title+"  ·  שלב "+stage+"/50",v->launch(g),false);}
    addButton(page,"חזרה",v->showHome(),false);setContentView(wrap(page));
  }

  private void addOrientationControls(LinearLayout target,String id) {
    TextView label=text("כיוון למסך של המשחק הזה",13,LIGHT);label.setGravity(Gravity.CENTER);target.addView(label,matchWrap());
    LinearLayout row=new LinearLayout(this);
    String[] values={"system","portrait","landscape"};String[] labels={"אוטומטי","לאורך","לרוחב"};
    for(int i=0;i<3;i++){final String value=values[i];Button b=smallButton(labels[i]);b.setOnClickListener(v->{prefs.edit().putString("orientation_"+id,value).apply();applyGameOrientation(id);toast("נשמר כיוון המסך למשחק");});row.addView(b,new LinearLayout.LayoutParams(0,44,1));}
    target.addView(row,matchWrap());
  }

  private void setGlobalOrientation(String value) {
    prefs.edit().putString("orientation_global",value).apply();
    applyOrientation(value);toast("הגדרת הכיוון נשמרה");
  }
  private void applyGameOrientation(String id) {
    String value=prefs.getString("orientation_"+id,prefs.getString("orientation_global","system"));
    applyOrientation(value);
  }
  private void applyOrientation(String value) {
    if(value.equals("portrait"))setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    else if(value.equals("landscape"))setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }
  private void toggleFullscreen() {
    boolean enabled=!prefs.getBoolean("fullscreen",false);
    prefs.edit().putBoolean("fullscreen",enabled).apply();
    applyFullscreen(enabled);
    if(currentGame!=null)renderGame();else if(page!=null)showSettings();
  }
  private void applySavedFullscreen(){applyFullscreen(prefs.getBoolean("fullscreen",false));}
  private void applyFullscreen(boolean enabled) {
    if(enabled)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  }

  private void finishStage(String message) {
    gamesPlayed++;prefs.edit().putInt("games_played",gamesPlayed).apply();
    if(currentGame!=null){int stage=prefs.getInt("stage_"+currentGame.id,1);if(stage<50)prefs.edit().putInt("stage_"+currentGame.id,stage+1).apply();}
    sound();
    new android.app.AlertDialog.Builder(this).setTitle("כל הכבוד!").setMessage(message).setPositiveButton("המשך", (d,w)->{if(currentGame!=null)launch(currentGame);}).setNegativeButton("לרשימה",(d,w)->showCatalog("הכול")).show();
  }

  private void initMemory() {
    memoryCards=new int[16];memoryShown=new boolean[16];memoryMatched=new boolean[16];memoryPairs=0;memoryA=-1;memoryB=-1;memoryBusy=false;
    for(int i=0;i<16;i++)memoryCards[i]=(i/2)+1;
    for(int i=15;i>0;i--){int j=random.nextInt(i+1);int t=memoryCards[i];memoryCards[i]=memoryCards[j];memoryCards[j]=t;}
  }
  private void init2048(){board2048=new int[4][4];score2048=0;add2048Tile();add2048Tile();}
  private void add2048Tile(){List<Integer> free=new ArrayList<>();for(int r=0;r<4;r++)for(int c=0;c<4;c++)if(board2048[r][c]==0)free.add(r*4+c);if(!free.isEmpty()){int x=free.get(random.nextInt(free.size()));board2048[x/4][x%4]=random.nextInt(10)==0?4:2;}}
  private boolean has2048(){for(int[] row:board2048)for(int x:row)if(x>=2048)return true;return false;}
  private void newMath(){mathA=random.nextInt(20)+1;mathB=random.nextInt(20)+1;mathAnswer=mathA+mathB;}
  private void initLights(){lights=new boolean[5][5];for(int i=0;i<10;i++)toggleLight(random.nextInt(5),random.nextInt(5));}
  private void toggleLight(int r,int c){lights[r][c]=!lights[r][c];if(r>0)lights[r-1][c]=!lights[r-1][c];if(r<4)lights[r+1][c]=!lights[r+1][c];if(c>0)lights[r][c-1]=!lights[r][c-1];if(c<4)lights[r][c+1]=!lights[r][c+1];}
  private boolean allLightsOff(){for(boolean[] row:lights)for(boolean x:row)if(x)return false;return true;}
  private int xoWinner(){int[][] lines={{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};for(int[] l:lines)if(xoBoard[l[0]]!=0&&xoBoard[l[0]]==xoBoard[l[1]]&&xoBoard[l[1]]==xoBoard[l[2]])return xoBoard[l[0]];return 0;}
  private boolean xoFull(){for(int x:xoBoard)if(x==0)return false;return true;}
  private void kickNoop(){}
  private int parse(String s,int fallback){try{return Integer.parseInt(s.trim());}catch(Exception e){return fallback;}}
  private void sound(){if(prefs.getBoolean("sound",true)&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,90);}catch(Exception ignored){}}
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

  private LinearLayout newPage(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(18,22,18,26);l.setBackgroundColor(BG);return l;}
  private ScrollView wrap(LinearLayout l){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(BG);s.addView(l);return s;}
  private void addHeader(LinearLayout p,String title,String subtitle){TextView t=text(title,28,Color.WHITE);t.setTypeface(null,Typeface.BOLD);t.setGravity(Gravity.CENTER);p.addView(t,new LinearLayout.LayoutParams(-1,76));TextView st=text(subtitle,15,LIGHT);st.setGravity(Gravity.CENTER);p.addView(st,matchWrap());addGap(p,14);}
  private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(8,8,8,8);return t;}
  private Button styledButton(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTextSize(primary?18:15);GradientDrawable shape=new GradientDrawable();shape.setColor(primary?GREEN:PANEL);shape.setCornerRadius(22);b.setBackground(shape);b.setPadding(8,7,8,7);return b;}
  private Button largeButton(String label){return styledButton(label,true);}
  private Button smallButton(String label){return styledButton(label,false);}
  private void addButton(LinearLayout p,String label,View.OnClickListener action,boolean primary){Button b=styledButton(label,primary);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT);lp.setMargins(0,6,0,6);p.addView(b,lp);b.setOnClickListener(action);}
  private EditText edit(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(LIGHT);e.setHint(hint);e.setTextSize(18);e.setPadding(14,8,14,8);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));return e;}
  private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(-1,LinearLayout.LayoutParams.WRAP_CONTENT);}
  private void addGap(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,h));}
  @Override public void onBackPressed(){if(puzzleWebView!=null){closeEmbeddedLibrary();return;}if(currentGame!=null)showCatalog("הכול");else showHome();}

  @Override protected void onDestroy(){if(puzzleWebView!=null){puzzleWebView.stopLoading();puzzleWebView.destroy();puzzleWebView=null;}if(tone!=null)tone.release();super.onDestroy();}

  private static class Game {
    final String id,title,category,description;
    Game(String i,String t,String c,String d){id=i;title=t;category=c;description=d;}
  }

  private static final String[][] WORD_Q = {
    {"ההפך מ'גדול' הוא…","קטן","רחב","גבוה"},
    {"מילה נרדפת ל'שמח' היא…","עליז","עייף","כבד"},
    {"מה משלים את המשפט: השמש זורחת ב…","בוקר","לילה","חורף"},
    {"ההפך מ'מהיר' הוא…","איטי","חזק","קל"},
    {"איזו מילה היא פרי?","תפוח","כיסא","חלון"},
    {"מה מתאים למילה 'ספרייה'?","ספרים","נעליים","כלים"},
    {"ההפך מ'פתוח' הוא…","סגור","מואר","רחוק"},
    {"איזו מילה קשורה לים?","גל","ענן","מדף"},
    {"מה בא אחרי יום שלישי?","רביעי","שני","שבת"},
    {"איזו מילה היא צבע?","ירוק","שקט","מהיר"}
  };
}
