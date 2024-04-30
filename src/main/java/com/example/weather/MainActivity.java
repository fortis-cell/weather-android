package com.example.weather;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {
    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV,conditionTV;
    private TextInputEditText cityEdt;
    private RecyclerView weatherRV;
    private ImageView iconIV, searchIV, backIV;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LocationManager locationManager;
    private int PERMISSION_CODE= 1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        homeRL = findViewById(R.id.idRLHome);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingPB = findViewById(R.id.idLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        cityEdt = findViewById(R.id.idEDCity);
        weatherRV = findViewById(R.id.idRVWeather);
        searchIV = findViewById(R.id.idIVSearch);
        backIV= findViewById(R.id.idIVHome);
        iconIV = findViewById(R.id.idIVIcon);
        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this,weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_CODE);

        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        cityName = getCityName(location.getLongitude(), location.getLatitude());
        getWeatherInfo(cityName);
        
        try {

        } catch (Exception e){
            Log.d(TAG, e.toString());
        }

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();

                if(city.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();

                } else {
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                    hideKeyboard(MainActivity.this);
                    cityEdt.clearFocus();
                }

            }
        });


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getWeatherInfo(cityName);

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please provide permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private String getCityName(double longitude, double latitude){
        String cityName2 = "Not found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);
            for(Address adr: addresses){
                String city = adr.getLocality();
                if(adr!=null){
                    if(city!=null && !city.equals("")){
                        cityName2 = city;
                    }
                }
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }

        return cityName2;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }



    private void getWeatherInfo(String cityName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=3178c8bc59e04510880162046230801&q="+cityName+"&days=1&aqi=yes&alerts=no";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( 0, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loadingPB.setVisibility(View.GONE);
                        homeRL.setVisibility(View.VISIBLE);
                        weatherRVModalArrayList.clear();

                        try {
                            String temperature = response.getJSONObject("current").getString("temp_c");
                            temperatureTV.setText(temperature + "°C");
                            int isDay = response.getJSONObject("current").getInt("is_day");
                            String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                            String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                            Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                            conditionTV.setText(condition);
                            if (isDay==1) {
                                // morning
                                Picasso.get().load("https://images.unsplash.com/photo-1517685352821-92cf88aee5a5?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1974&q=80").into(backIV);
                            } else {
                                Picasso.get().load("https://images.unsplash.com/photo-1630691432568-b202e42643e2?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=1945&q=80").into(backIV);
                            }
                            JSONObject forecastObj = response.getJSONObject("forecast");
                            JSONObject forcast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                            JSONArray hourArray = forcast0.getJSONArray("hour");



                            for (int i = 0; i < hourArray.length(); i++) {
                                JSONObject hourObj = hourArray.getJSONObject(i);
                                String time = hourObj.getString("time");
                                String temp = hourObj.getString("temp_c");
                                String img = hourObj.getJSONObject("condition").getString("icon").substring(2);

                                String wind = hourObj.getString("wind_kph");
                                weatherRVModalArrayList.add(new WeatherRVModal(time, temp, img, wind));

                            }
                            weatherRVAdapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Toast.makeText(MainActivity.this, "Please enter a valid city name", Toast.LENGTH_SHORT).show();
                        Log.d("TAG", "error here");
                        Log.e("VolleyError", error.getMessage());
                    }
                });
        requestQueue.add(jsonObjectRequest);

    }


}