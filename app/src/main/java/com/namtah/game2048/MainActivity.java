package com.namtah.game2048;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    TextView selectFour, selectFive, selectSix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectFour = findViewById(R.id.selectFour);
        selectFive = findViewById(R.id.selectFive);
        selectSix = findViewById(R.id.selectSix);

        selectFour.setOnClickListener(this);
        selectFive.setOnClickListener(this);
        selectSix.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.selectFour) {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("base", 4);
            startActivity(intent);
        } else if (v.getId() == R.id.selectFive) {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("base", 5);
            startActivity(intent);
        } else if (v.getId() == R.id.selectSix) {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("base", 16);
            startActivity(intent);
        }
    }
}
