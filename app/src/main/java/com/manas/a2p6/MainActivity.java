package com.manas.a2p6;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public GoogleMap mMap;
    Button locateBtn;
    Button weatherBtn;
    TextView locationField;
    TextView tempratureField;
    public double latitude = 0.00;
    public double longitude = 0.00;
    private static final int REQUEST_CODE_LOC_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locateBtn = (Button) findViewById(R.id.locateButton);
        weatherBtn = (Button) findViewById(R.id.getTempratureButton);
        locationField = findViewById(R.id.location_field);
        tempratureField = findViewById(R.id.currentTemprature_field);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MapsFragment()).commit();

        locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Also sets the location globally
                //subsequently sets tge location on the map in fragment
                getpermission();
            }
        });
        
        weatherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWeather();
            }
        });


    }

    private void updateWeather() {
        //Since this is network request
        //We need to put it on an async queue to be processed
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = getUrl();

        JsonObjectRequest jsonObjectRequest = new
                JsonObjectRequest(Request.Method.GET, url,
                new JSONObject(),
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String server_data = response.toString();
                            JSONObject json_outer_obj = new JSONObject(server_data);
                            JSONObject currentValue = json_outer_obj.getJSONObject("current");
                            JSONObject json_inner_current_obj = new JSONObject(currentValue.toString());
                            String currentTemp = json_inner_current_obj.getString("temp");
                            String currentPressure = json_inner_current_obj.getString("pressure");
                            String currentWindSpeed = json_inner_current_obj.getString("wind_speed");
                            String humidity = json_inner_current_obj.getString("humidity");
                            String windDirection = json_inner_current_obj.getString("wind_deg");
                            Double tempInCelcius = Double.valueOf(currentTemp) - 273.15;

                            String result = "Temperature: " + tempInCelcius + "\n" +
                                    "Pressure: " + currentPressure + "\n" +
                                    "Humidity: " + humidity + "\n" +
                                    "Wind Direction: " + windDirection + "\n" +
                                    "Wind Speed: " + currentWindSpeed;

                            tempratureField.setText(String.valueOf(tempInCelcius));
                            Log.e("data", result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", error.toString());
            }
        });
        queue.add(jsonObjectRequest);

    }

    //Gets the url to be called based on the current location
    private String getUrl() {
        return  "https://api.openweathermap.org/data/2.5/onecall?lat=LATTT&lon=LONGGG&appid=ab57f83f7f35c8c4355d9fb49b2d0f96"
                .replace("LATTT",String.valueOf(this.latitude))
                .replace("LONGGG",String.valueOf(this.longitude));
    }

    private void getpermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOC_PERMISSION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(3000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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
            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {

                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .removeLocationUpdates(this);
                            if (locationResult != null && locationResult.getLocations().size() > 0) {
                                int latestLocationIndex = locationResult.getLocations().size() - 1;
                                latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                                longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                                setLocationGlobally(latitude,longitude);
                                // Add a marker in present cordinates and move the camera
//                            LatLng presentCordinates = new LatLng(latitude, longitude);
//                            mMap.addMarker(new MarkerOptions().position(presentCordinates).title("Marker in Present location"));
//                            mMap.moveCamera(CameraUpdateFactory.newLatLng(presentCordinates));
                                broadcastMessageToMapFragment(latitude, longitude);
                                setLocationOnActivity(latitude, longitude);
                            }
                        }
                    }, Looper.getMainLooper());
        }
    }

    private void setLocationGlobally(double latitude, double longitude) {
        this.latitude=latitude;
        this.longitude=longitude;
    }

    private void setLocationOnActivity(double latitude, double longitude) {
        locationField.setText(String.valueOf(latitude)+ ", "+ longitude);
    }

    private void broadcastMessageToMapFragment(double latitude, double longitude) {
        Intent intent = new Intent("STRING_ID_FOR_BRODCAST");
        intent.putExtra("latitude", String.valueOf(latitude));
        intent.putExtra("longitude", String.valueOf(longitude));
        sendBroadcast(intent);
    }
}