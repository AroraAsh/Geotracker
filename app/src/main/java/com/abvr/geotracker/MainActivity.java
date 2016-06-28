package com.abvr.geotracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static MainActivity ins;
    TextView[] locationViews = new TextView[5];
    TextView currentLocationTextView;
    String[] locations = {"Pidilite Industries Limited","Andheri Metro Station",
                          "Shoppers Stop, Andheri West","AWHO, Sandeep Vihar",
                          "Forum Value Mall"};
    String notAtLocation = "You are not at Any location";
    int[] locationIds = {R.id.location1TextView,R.id.location2TextView,
                         R.id.location3TextView,R.id.location4TextView,R.id.location5TextView};
    static final String AT_LOCATION = "AT_LOCATION";
    static final String CURRENT_LOCATION_VALUE = "CURRENT_LOCATION_VALUE";
    static final String DISTANCE = "DISTANCE";
    public final String BROADCAST_ACTION = "vr.com.assignment1.BROADCAST_LOCATION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }
        if (Build.VERSION.SDK_INT<=Build.VERSION_CODES.LOLLIPOP_MR1){
            new AsyncTask<Void,Void,Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    Log.d("Service","In AsyncTask");
                    Intent intent = new Intent(MainActivity.this,LocationCheckService.class);
                    startService(intent);
                    return null;
                }
            }.execute();
        }
        Log.e("On","Create" + Build.VERSION.SDK_INT);
        setUpViews();
        ins=this;

    }

    static MainActivity getInstance(){
        return ins;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0
                        && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Intent intent = new Intent(this,LocationCheckService.class);
                    startService(intent);
                }else{
                    Toast.makeText(this,"Cannot function Without Location",Toast.LENGTH_LONG).show();
                }
        }
    }

    void setUpViews(){
        for(int i=0;i<5;i++){
            locationViews[i] = (TextView) findViewById(locationIds[i]);
        }
        currentLocationTextView = (TextView) findViewById(R.id.currentLocationTextView);
        updateLocations();
    }

    public void updateLocations(){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Update","Locations");
                TinyDB tinyDB = new TinyDB(getApplicationContext());
                if (tinyDB.getBoolean(AT_LOCATION,false)){
                    int i = tinyDB.getInt(CURRENT_LOCATION_VALUE,-1);
                    if (i!=-1) {
                        String temp = getResources().getString(R.string.atLocationText);
                        temp = temp + " " + locations[i];
                        currentLocationTextView.setText(Html.fromHtml("<b>"+temp+"</b>"));
                    }
                }else
                    currentLocationTextView.setText(Html.fromHtml("<b>"+notAtLocation+"</b>"));
                for (int j=0;j<5;j++) {
                    double dist = tinyDB.getDouble(MainActivity.DISTANCE + j, 0);
                    dist = dist/1000;
                    String text = dist + " kms from " + locations[j];
                    locationViews[j].setText(Html.fromHtml("<b>"+text));
                }
            }
        });

    }



    public static class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Receiver","Inside");
        MainActivity.getInstance().updateLocations();
        }
    }


}
