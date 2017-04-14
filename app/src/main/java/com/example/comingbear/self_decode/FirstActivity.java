package com.example.comingbear.self_decode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by ComingBear on 2017/4/1.
 */

public class FirstActivity extends Activity {
    private String filePath1 = "/sdcard/tc10.h264";
    private String filePath2 = "/sdcard/slamtv10.h264";
    private String filePath3 = "/sdcard/slamtv60.h264";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        initWidgets();
    }

    private void initWidgets() {
        Button button1 = (Button) findViewById(R.id.button1);//l;
        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(FirstActivity.this, MainActivity.class);
                intent.putExtra("extra_data",filePath1);
                startActivity(intent);
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);//l;
        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setClass(FirstActivity.this, MainActivity.class);
                intent.putExtra("extra_data",filePath2);
                startActivity(intent);
            }
        });

        Button button3 = (Button) findViewById(R.id.button3);//l;
        button3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setClass(FirstActivity.this, MainActivity.class);
                intent.putExtra("extra_data",filePath3);
                startActivity(intent);
            }
        });
    }
}
