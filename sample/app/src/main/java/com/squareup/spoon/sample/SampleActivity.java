package com.squareup.spoon.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SampleActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sample);

    // When clicked, set text.
    findViewById(R.id.click_me).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        TextView tv = (TextView) findViewById(R.id.say_hello);
        tv.setText(R.string.hello);
      }
    });
  }
}
