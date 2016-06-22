package vr.com.assignment1;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.math.BigDecimal;

public class LocationCheckService extends Service implements LocationListener {
    LocationManager manager;
    String TAG = "LocationCheckService";
    private static final int TWO_MINUTES = 1000 * 60 * 3;
    public final String BROADCAST_ACTION = "vr.com.assignment1.BROADCAST_LOCATION";
    TinyDB tinyDB ;
    Location lastLocation;
    long LOCATION_INTERVAL = 5000;
    float LOCATION_DISTANCE = 10;
    String[] locations = {"Pidilite Industries Limited", "Andheri Metro Station",
            "Shoppers Stop, Andheri West", "AWHO, Sandeep Vihar",
            "Forum Value Mall"};
    double[][] latLongs = new double[][]{{19.115421, 72.869691},
            {19.120628, 72.848491}, {19.115218, 72.842487},
            {13.022401, 77.759835}, {12.959504, 77.747890}};

    double MIN_DISTANCE = 5;

    public LocationCheckService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        tinyDB = new TinyDB(getApplicationContext());
        Log.e("Service","Created");

            manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.removeUpdates(this);
    }

    public boolean isLocationBetter(Location location){
        Log.e("Location","CheckBetter");
        if (null==lastLocation){
            return true;
        }
        long timeDelta = location.getTime() - lastLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - lastLocation.getAccuracy());
        return accuracyDelta < 0;

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("Location","Changed");
        if (isLocationBetter(location)) {
            Log.e("Location","Is Better");
            lastLocation = new Location(location);
            Intent intent = new Intent();
            intent.setAction(BROADCAST_ACTION);
            sendBroadcast(intent);
            checkNewLocation();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void checkNewLocation(){
        double[] distances = findDistance();
        tinyDB.putBoolean(MainActivity.AT_LOCATION,false);
        for (int i=0;i<5;i++){
            if (distances[i]<=MIN_DISTANCE){
                tinyDB.putBoolean(MainActivity.AT_LOCATION,true);
                tinyDB.putInt(MainActivity.CURRENT_LOCATION_VALUE,i);
                tinyDB.putDouble(MainActivity.DISTANCE+i,0);
                showNotification(i);
                break;
            }
        }
    }

    public double[] findDistance(){
        double[] distances = new double[5];
        for (int i=0;i<5;i++){
            Location locationA = new Location("A");
            locationA.setLatitude(latLongs[i][0]);
            locationA.setLongitude(latLongs[i][1]);
            distances[i] = lastLocation.distanceTo(locationA);
            Double precisionSet = new BigDecimal(distances[i])
                    .setScale(3,BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            tinyDB.putDouble(MainActivity.DISTANCE+i,precisionSet);
        }
        return distances;
    }

    public void showNotification(int i){
        Log.e("Notification","Shown");
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent intentP = PendingIntent.getActivity(this,0,intent,0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location Update")
                .setContentText("You have Just arrived at " + locations[i])
                .setSmallIcon(R.drawable.location)
                .setContentIntent(intentP)
                .setAutoCancel(true).build();
        notificationManager.notify(i,notification);

    }
}
