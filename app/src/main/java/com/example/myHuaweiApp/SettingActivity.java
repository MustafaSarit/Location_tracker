package com.example.myHuaweiApp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

public class SettingActivity extends AppCompatActivity {

    Button save;
    RadioGroup radioGroup;
    RadioButton radioButton;
    ImageView imageView;
    FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        imageView = findViewById(R.id.profile);
        firebaseAuth = FirebaseAuth.getInstance();

        Picasso.get().load(firebaseAuth.getCurrentUser().getPhotoUrl())
                .transform(new CircleTransform(100,0))
                .into(imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingActivity.this, ProfileActivity.class));
            }
        });

        SharedPreferences settings = getSharedPreferences("Theme", 0);
        boolean silent = settings.getBoolean("DarkTheme", false);
        if(silent){
            radioButton = findViewById(R.id.dark);
        }else {
            radioButton = findViewById(R.id.light);
        }
        radioButton.setChecked(true);

        save = findViewById(R.id.save);
        radioGroup = findViewById(R.id.radioGroup);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String theme = (String) radioButton.getText();
                SharedPreferences settings = getSharedPreferences("Theme", 0);
                SharedPreferences.Editor editor = settings.edit();
                switch (theme){
                    case "Dark Theme":
                        editor.putBoolean("DarkTheme", true);
                        editor.apply();
                        Toast.makeText(SettingActivity.this,"Dark theme on use", Toast.LENGTH_SHORT).show();
                        break;
                    case "Light Theme":
                        editor.putBoolean("DarkTheme", false);
                        editor.apply();
                        Toast.makeText(SettingActivity.this,"Light theme on use", Toast.LENGTH_SHORT).show();
                        break;
                }
                startActivity(new Intent(SettingActivity.this, MapsActivity.class));
            }
        });
    }

    // Gets check radio buttons
    public void checkButton(View v){
        int radioId = radioGroup.getCheckedRadioButtonId();
        radioButton = findViewById(radioId);
    }

    // Maps button onClick function
    public void toMaps(final View view){
        startActivity(new Intent(SettingActivity.this, MapsActivity.class));
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to exit!")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        finishAffinity();
                        System.exit(0);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}