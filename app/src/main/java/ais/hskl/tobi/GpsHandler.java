package ais.hskl.tobi;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GpsHandler implements LocationListener {

    private Context context;
    private SpeedChangedListener speedListener;

    public GpsHandler(Context context, SpeedChangedListener speedListener)
    {
        this.speedListener = speedListener;
        this.context = context;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private double lastLat;
    private double lastLng;
    private long lastTimestamp;

    @Override
    public void onLocationChanged(Location location) {

        long currentStamp = System.currentTimeMillis();
        if(lastLat > 0 && lastLng > 0) {
            String msg = String.format("Speed: %f", distanceByGeo(lastLat, lastLng, location.getLatitude(), location.getLongitude(), currentStamp, this.lastTimestamp));
            Toast.makeText(GpsHandler.this.context, msg, Toast.LENGTH_LONG).show();
        }

        this.lastLat = location.getLatitude();
        this.lastLng = location.getLongitude();
        this.lastTimestamp = currentStamp;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private static double distanceByGeo(double lat1, double lng1, double lat2, double lng2, long timestampFirst, long timestampSecond){

        float[] result = new float[2];
        Location.distanceBetween(lat1, lng1, lat2, lng2, result);

        return ((result[0] / (timestampSecond - timestampFirst)) * 3.6);
    }
}
