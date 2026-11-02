package com.gamebox.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
  private int green = Color.rgb(20, 65, 43);
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(8, 25, 17));
    getWindow().setNavigationBarColor(Color.rgb(8, 25, 17));
    LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setGravity(Gravity.CENTER); root.setPadding(28,28,28,28); root.setBackgroundColor(Color.rgb(8,25,17));
    TextView title = new TextView(this); title.setText("GAME BOX"); title.setTextColor(Color.WHITE); title.setTextSize(32); title.setGravity(Gravity.CENTER); root.addView(title, new LinearLayout.LayoutParams(-1,120));
    TextView sub = new TextView(this); sub.setText("אוסף המשחקים שלך"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(18); sub.setGravity(Gravity.CENTER); root.addView(sub);
    addButton(root,"משחקים",v -> showMessage("מאגר המשחקים יתווסף בשלבי הפיתוח הבאים"));
    addButton(root,"הגדרות תצוגה",v -> showMessage("הגדרות מסך מלא וכיוון מסך יתווספו בהמשך"));
    addButton(root,"אודות",v -> showMessage("GAME BOX · גרסת פיתוח"));
    setContentView(root);
  }
  private void addButton(LinearLayout root,String label,View.OnClickListener listener){ Button b=new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,62); p.setMargins(0,14,0,0); root.addView(b,p); b.setOnClickListener(listener); }
  private void showMessage(String s){ new android.app.AlertDialog.Builder(this).setMessage(s).setPositiveButton("אישור",null).show(); }
}
