package ais.hskl.tobi;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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

    @Override
    public void onLocationChanged(Location location) {

        if(lastLat > 0 && lastLng > 0) {
            String msg = String.format("Speed: %f", distanceByGeo(lastLat, lastLng, location.getLatitude(), location.getLongitude()));
            Toast.makeText(GpsHandler.this.context, msg, Toast.LENGTH_LONG).show();
        }

        this.lastLat = location.getLatitude();
        this.lastLng = location.getLongitude();
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

    private static double r = 6378100;
    private static double distanceByGeo(double lat1, double lng1, double lat2, double lng2){
        lat1 = lat1 * Math.PI / 180.0;
        lng1 = lng1 * Math.PI / 180.0;

        lat2 = lat2 * Math.PI / 180.0;
        lng2 = lng2 * Math.PI / 180.0;

        double rho1 = r * Math.cos(lat1);
        double z1 = r * Math.sin(lat1);

        double x1 = rho1 * Math.cos(lng1);
        double y1 = rho1 * Math.sin(lng1);

        // Q
        double rho2 = r * Math.cos(lat2);
        double z2 = r * Math.sin(lat2);
        double x2 = rho2 * Math.cos(lng2);
        double y2 = rho2 * Math.sin(lng2);

        // Dot product
        double dot = (x1 * x2 + y1 * y2 + z1 * z2);
        double cos_theta = dot / (r * r);

        double theta = Math.acos(cos_theta);

        // Distance in Metres
        return r * theta;
    }
}
