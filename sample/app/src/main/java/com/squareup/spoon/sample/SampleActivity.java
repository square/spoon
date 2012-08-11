package com.squareup.spoon.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SampleActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sample);
    final TextView tv = (TextView) findViewById(R.id.say_hello);

    // When clicked, set text.
    findViewById(R.id.click_me1).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        tv.setText(R.string.hello1);
      }
    });
    findViewById(R.id.click_me2).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        tv.setText(R.string.hello2);
      }
    });
    findViewById(R.id.click_me3).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        tv.setText(R.string.hello3);
      }
    });
  }
}
