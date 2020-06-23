package ru.ifsoft.network;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.facebook.FacebookSdk;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.ifsoft.network.app.App;
import ru.ifsoft.network.common.ActivityBase;
import ru.ifsoft.network.util.CustomRequest;

public class AppActivity extends ActivityBase {

    Button loginBtn, signupBtn;

    ProgressBar progressBar;

    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        FacebookSdk.sdkInitialize(getApplicationContext());

        // Get Firebase token

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(AppActivity.this, new OnSuccessListener<InstanceIdResult>() {

            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {

                String token = instanceIdResult.getToken();

                App.getInstance().setGcmToken(token);

                Log.d("FCM Token", token);
            }
        });

        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation().addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {

                    if (task.isSuccessful() && task.getResult() != null) {

                        mLastLocation = task.getResult();

                        // Set geo data to App class

                        App.getInstance().setLat(mLastLocation.getLatitude());
                        App.getInstance().setLng(mLastLocation.getLongitude());

                        // Save data

                        App.getInstance().saveData();

                        // Send location data to server

                        App.getInstance().setLocation();

                    } else {

                        Log.d("GPS", "AppActivity getLastLocation:exception", task.getException());
                    }
                }
            });
        }

        loginBtn = (Button) findViewById(R.id.loginBtn);
        signupBtn = (Button) findViewById(R.id.signupBtn);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(AppActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(AppActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                window.setStatusBarColor(getColor(R.color.app_bg));

            } else {

                window.setStatusBarColor(getApplicationContext().getResources().getColor(R.color.app_bg));
            }
        }
    }

    @Override
    protected void  onStart() {

        super.onStart();

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0) {

            showLoadingScreen();

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_AUTHORIZE, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            if (App.getInstance().authorize(response)) {

                                if (App.getInstance().getState() == ACCOUNT_STATE_ENABLED) {

                                    App.getInstance().updateGeoLocation();

                                    Intent intent = new Intent(AppActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);

                                } else {

                                    showContentScreen();
                                    App.getInstance().logout();
                                }

                            } else {

                                showContentScreen();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    showContentScreen();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("clientId", CLIENT_ID);
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());
                    params.put("appType", Integer.toString(APP_TYPE_ANDROID));
                    params.put("fcm_regId", App.getInstance().getGcmToken());

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);

        } else {

            showContentScreen();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    public void showContentScreen() {

        progressBar.setVisibility(View.GONE);

        loginBtn.setVisibility(View.VISIBLE);
        signupBtn.setVisibility(View.VISIBLE);
    }

    public void showLoadingScreen() {

        progressBar.setVisibility(View.VISIBLE);

        loginBtn.setVisibility(View.GONE);
        signupBtn.setVisibility(View.GONE);
    }
}
