package com.example.myapplication;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.Maps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

public class maps extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap; // ✅ Affecte l'instance reçue à ta variable

        LatLng emsiRoudani = new LatLng(33.5849, -7.6244); // Coordonnées approximatives
        LatLng emsiCentre1 = new LatLng(33.5895, -7.6120);
        LatLng emsiCentre2 = new LatLng(33.5890, -7.6115);
        LatLng emsiOrangers = new LatLng(33.5735, -7.6200);
        LatLng emsiMoulayYoussef = new LatLng(33.5890, -7.6200);
        LatLng emsiCFC = new LatLng(33.5660, -7.6200);
        Marker marker = mMap.addMarker(new MarkerOptions().position(emsiRoudani).title("EMSI Roudani"));
        mMap.addMarker(new MarkerOptions().position(emsiCentre1).title("EMSI Centre"));
        mMap.addMarker(new MarkerOptions().position(emsiCentre2).title("EMSI Centre 2"));
        mMap.addMarker(new MarkerOptions().position(emsiOrangers).title("EMSI Les Orangers"));
        mMap.addMarker(new MarkerOptions().position(emsiMoulayYoussef).title("EMSI Moulay Youssef"));
        mMap.addMarker(new MarkerOptions().position(emsiCFC).title("EMSI Casa Finance City"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(emsiCentre1, 10));
        marker.setTag("destination");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                Toast.makeText(maps.this, "Marker clicked!", Toast.LENGTH_SHORT).show();
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(maps.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }

                Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    LatLng destination = marker.getPosition();

                    mMap.addPolyline(new PolylineOptions()
                            .add(current, destination)
                            .width(5)
                            .color(Color.BLUE));
                }

                return true;
            }
        });
    }

}


