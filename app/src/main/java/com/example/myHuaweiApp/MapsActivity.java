package com.example.myHuaweiApp;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, LocationListener {

    private GoogleMap mMap;
    private FirebaseAuth firebaseAuth;
    Button settings;
    LocationManager locationManager;
    Location initLoc;
    CameraPosition cameraPosition;
    Bitmap myBitmap;
    MarkerOptions mo;
    Marker marker;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    float distance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initLoc = null;
        settings = findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MapsActivity.this, SettingActivity.class));
            }
        });
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocation();


        firebaseAuth = FirebaseAuth.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

        SharedPreferences settings = getSharedPreferences("Theme", 0);
        boolean theme = settings.getBoolean("DarkTheme", false);
        if (theme) {
            try {
                boolean success = mMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                                this, R.raw.dark_theme));

                if (!success) {
                    Log.e("TAG", "Style parsing failed.");
                }
            } catch (Resources.NotFoundException e) {
                Log.e("TAG", "Can't find style. Error: ", e);
            }
        }

    }

    // If user click marker icon app will direct user to profile page
    @Override
    public boolean onMarkerClick(Marker marker) {
        startActivity(new Intent(MapsActivity.this, ProfileActivity.class));
        return true;
    }

    // This function will get initial location of the user then compare it with current location
    // Marker will show current location
    // Program will show a dialog if user gets away from initial location
    // Distance to see dialog is 1 meter and can be changed without changing code in client side
    @Override
    public void onLocationChanged(Location location) {
        TextView view = findViewById(R.id.textView3);
        String text = "Lat is: " + location.getLatitude() + "\nLng is: " + location.getLongitude();
        view.setText(text);
        if(initLoc == null){
            initLoc = location;
            Picasso.get()
                    .load(firebaseAuth.getCurrentUser().getPhotoUrl())
                    .transform(new CircleTransform(100, 0))
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            myBitmap = bitmap;
                            LatLng initialLoc = new LatLng(initLoc.getLatitude(), initLoc.getLongitude());
                            mo =  new MarkerOptions().position(initialLoc).icon(BitmapDescriptorFactory.fromBitmap(myBitmap));
                            cameraPosition = new CameraPosition.Builder()
                                    .target(initialLoc)
                                    .zoom(17)
                                    .build();
                            marker = mMap.addMarker(mo);
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {}
                    });
        }else {
            distance = location.distanceTo(initLoc);
            LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
            marker.setPosition(coordinate);
            cameraPosition = new CameraPosition.Builder()
                    .target(coordinate)
                    .zoom(17)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.child("distance").getValue(Integer.class)<distance){
                        Toast.makeText(MapsActivity.this, "Please get back to original position", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}
    @Override
    public void onProviderEnabled(String s) {}
    @Override
    public void onProviderDisabled(String s) {}

    // Request location with specified criteria.
    private void requestLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider, 0, 0, this);
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