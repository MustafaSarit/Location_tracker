package com.example.myHuaweiApp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    private static final int Google_Sign_Code = 10005;
    SignInButton gButton;
    GoogleSignInOptions gso;
    GoogleSignInClient gClient;
    FirebaseAuth firebaseAuth;
    CallbackManager callbackManager;
    LoginButton loginButton;
    int total;
    int dark;
    private int locationPermission = 1;
    SharedPreferences sharedPreferences = null;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    AccessToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusCheck();

        //Check First Run
        sharedPreferences = getSharedPreferences("Theme", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("First_Run", true)){
            //Decide default theme for the user %60 dark %40 light
            decideTheme();
            sharedPreferences.edit().putBoolean("First_Run", false).apply();
        }



        if(ContextCompat.checkSelfPermission(Login.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermission();
        }

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null){
            startActivity(new Intent(Login.this, MapsActivity.class));
        }



        setContentView(R.layout.activity_login);
        gButton = findViewById(R.id.signG);
        loginButton = findViewById(R.id.signF);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("922776001405-n8i6g3nf4k8t1equho5keg6l7c7f0l70.apps.googleusercontent.com")
                .requestEmail()
                .build();

        gClient = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);

        gButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sign = gClient.getSignInIntent();
                startActivityForResult(sign, Google_Sign_Code);
            }
        });

        //Facebook
        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() { }
            @Override
            public void onError(FacebookException error) { }
        });

    }

    /* This function decide default theme for the new users. It checks database for dark theme percentage
    *  if the percentage is lower than 60 it will make default theme dark, and light if not.*/
    private void decideTheme() {
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                total = snapshot.child("total").getValue(Integer.class);
                dark = snapshot.child("dark_theme").getValue(Integer.class);
                if(total == 0) {
                    sharedPreferences.edit().putBoolean("DarkTheme", true).apply();
                    myRef.child("dark_theme").setValue(dark+1);
                    myRef.child("total").setValue(total+1);
                    return;
                }
                int perc = (100/total) * dark;
                if (perc <= 60) {
                    sharedPreferences.edit().putBoolean("DarkTheme", true).apply();
                    myRef.child("dark_theme").setValue(dark+1);
                    myRef.child("total").setValue(total+1);
                }
                else{
                    sharedPreferences.edit().putBoolean("DarkTheme",false).apply();
                    myRef.child("total").setValue(total+1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    // Checks if location service is enabled.
    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }
    // If user don't enable location service app will be terminated.
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        finish();
                        System.exit(0);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Asks user for location permission.
    private void requestPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            ActivityCompat.requestPermissions(Login.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, locationPermission);
        }else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, locationPermission);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == locationPermission){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                finishAffinity();
                System.exit(0);
            }
        }
    }

    // Get Facebook accessToken for later uses.
    private void handleFacebookAccessToken(AccessToken accessToken) {
        mToken = accessToken;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Google_Sign_Code){
            Task<GoogleSignInAccount> signInTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount signInAcc = signInTask.getResult(ApiException.class);
                AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAcc.getIdToken(), null);
                firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                    }
                });
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }else {
            AuthCredential authCredential = FacebookAuthProvider.getCredential(mToken.getToken());
            firebaseAuth.signInWithCredential(authCredential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                        }
                    });
        }
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