package ru.ifsoft.network.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.FirebaseApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.ifsoft.network.R;
import ru.ifsoft.network.adapter.BaseGift;
import ru.ifsoft.network.adapter.FeelingsListAdapter;
import ru.ifsoft.network.constants.Constants;
import ru.ifsoft.network.model.Feeling;
import ru.ifsoft.network.util.CustomRequest;
import ru.ifsoft.network.util.LruBitmapCache;

public class App extends MultiDexApplication implements Constants {

	public static final String TAG = App.class.getSimpleName();

	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;

	private static App mInstance;

    private ArrayList<Feeling> feelingsList;
    private ArrayList<BaseGift> giftsList;

    private SharedPreferences sharedPref;

    private String username, fullname, accessToken, gcmToken = "", fb_id = "", photoUrl, coverUrl, area = "", country = "", city = "";
    private Double lat = 0.000000, lng = 0.000000;
    private long id;
    private int state, allowRewardedAds = 1, admob = 1, ghost, pro, verify, balance, allowShowMyInfo, allowShowMyFriends, allowShowMyGallery, allowShowMyGifts, allowGalleryComments, allowComments, allowMessages, allowLikesGCM, allowCommentsGCM, allowFollowersGCM, allowGiftsGCM, allowMessagesGCM, allowCommentReplyGCM, errorCode, currentChatId = 0, notificationsCount = 0, messagesCount = 0, guestsCount = 0, newFriendsCount = 0;

	@Override
	public void onCreate() {

		super.onCreate();
        mInstance = this;

        FirebaseApp.initializeApp(this);

        sharedPref = this.getSharedPreferences(getString(R.string.settings_file), Context.MODE_PRIVATE);

        this.readData();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {

            try {

                ProviderInstaller.installIfNeeded(this);

            } catch (Exception e) {

                e.getMessage();
            }
        }
	}

    public void setLocation() {

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0) {

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_SET_GEO_LOCATION, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!response.getBoolean("error")) {

//                                            Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {

                                e.printStackTrace();

                            } finally {

                                Log.d("Set GEO Success", response.toString());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.d("Set GEO Error", error.toString());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());
                    params.put("lat", Double.toString(App.getInstance().getLat()));
                    params.put("lng", Double.toString(App.getInstance().getLng()));

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);
        }
    }

    public void getAddress(Double lat, Double lng) {

        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getApplicationContext(), Locale.US);

        try {

            addresses = geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && addresses.size() > 0) {

                App.getInstance().setCity(addresses.get(0).getLocality());
                App.getInstance().setArea(addresses.get(0).getAdminArea());
                App.getInstance().setCountry(addresses.get(0).getCountryName());

                if (App.getInstance().getCity().length() == 0) {

                    App.getInstance().setCity("Unknown");
                }

                if (App.getInstance().getArea().length() == 0) {

                    App.getInstance().setArea("Unknown");
                }

                if (App.getInstance().getCountry().length() == 0) {

                    App.getInstance().setCountry("Unknown");
                }

            } else {

                App.getInstance().setCity("Unknown");
                App.getInstance().setArea("Unknown");
                App.getInstance().setCountry("Unknown");
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    
    public boolean isConnected() {
    	
    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();

    	if (netInfo != null && netInfo.isConnectedOrConnecting()) {

    		return true;
    	}

    	return false;
    }

    public void logout() {

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0) {

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_LOGOUT, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!response.getBoolean("error")) {



                                }

                            } catch (JSONException e) {

                                e.printStackTrace();

                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    App.getInstance().removeData();
                    App.getInstance().readData();
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("clientId", CLIENT_ID);
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);

        }

        App.getInstance().removeData();
        App.getInstance().readData();
    }

    public void getSettings() {

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0) {

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_GET_SETTINGS, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!response.getBoolean("error")) {

                                    if (response.has("messagesCount")) {

                                        App.getInstance().setMessagesCount(response.getInt("messagesCount"));
                                    }

                                    if (response.has("notificationsCount")) {

                                        App.getInstance().setNotificationsCount(response.getInt("notificationsCount"));
                                    }

                                    if (response.has("guestsCount")) {

                                        App.getInstance().setGuestsCount(response.getInt("guestsCount"));
                                    }

                                }

                            } catch (JSONException e) {

                                e.printStackTrace();

                            } finally {

                                Log.d("App getSettings()", response.toString());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.e("App getSettings()", error.toString());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("clientId", CLIENT_ID);
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());
                    params.put("lat", Double.toString(App.getInstance().getLat()));
                    params.put("lng", Double.toString(App.getInstance().getLng()));

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);
        }
    }

    public void updateGeoLocation() {

        if (App.getInstance().isConnected() && App.getInstance().getId() != 0 && App.getInstance().getLat() == 0.000000 && App.getInstance().getLat() == 0.000000) {

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_SET_GEO_LOCATION, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!response.getBoolean("error")) {

//                                    Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {

                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.e("updateGeoLocation()", error.toString());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());
                    params.put("lat", String.format(Locale.ENGLISH, "%f", App.getInstance().getLat()));
                    params.put("lng", "");

                    return params;
                }
            };

            App.getInstance().addToRequestQueue(jsonReq);
        }
    }

    public Boolean authorize(JSONObject authObj) {

        try {

            if (authObj.has("error_code")) {

                this.setErrorCode(authObj.getInt("error_code"));
            }

            if (!authObj.has("error")) {

                return false;
            }

            if (authObj.getBoolean("error")) {

                return false;
            }

            if (!authObj.has("account")) {

                return false;
            }

            JSONArray accountArray = authObj.getJSONArray("account");

            if (accountArray.length() > 0) {

                JSONObject accountObj = (JSONObject) accountArray.get(0);

                if (accountObj.has("pro")) {

                    this.setPro(accountObj.getInt("pro"));

                } else {

                    this.setPro(0);
                }

                this.setUsername(accountObj.getString("username"));
                this.setFullname(accountObj.getString("fullname"));
                this.setState(accountObj.getInt("state"));
                this.setAdmob(accountObj.getInt("admob"));
                this.setGhost(accountObj.getInt("ghost"));
                this.setVerify(accountObj.getInt("verify"));
                this.setBalance(accountObj.getInt("balance"));
                this.setFacebookId(accountObj.getString("fb_id"));
                this.setAllowComments(accountObj.getInt("allowComments"));

                if (accountObj.has("allowGalleryComments")) {

                    this.setAllowGalleryComments(accountObj.getInt("allowGalleryComments"));
                }

                this.setAllowMessages(accountObj.getInt("allowMessages"));

                this.setPhotoUrl(accountObj.getString("lowPhotoUrl"));
                this.setCoverUrl(accountObj.getString("coverUrl"));

                this.setAllowShowMyInfo(accountObj.getInt("allowShowMyInfo"));
                this.setAllowShowMyFriends(accountObj.getInt("allowShowMyFriends"));
                this.setAllowShowMyGifts(accountObj.getInt("allowShowMyGifts"));

                if (accountObj.has("allowShowMyGallery")) {

                    this.setAllowShowMyGallery(accountObj.getInt("allowShowMyGallery"));
                }

                if (App.getInstance().getLat() == 0.000000 && App.getInstance().getLng() == 0.000000) {

                    this.setLat(accountObj.getDouble("lat"));
                    this.setLng(accountObj.getDouble("lng"));
                }
            }

            this.setId(authObj.getLong("accountId"));
            this.setAccessToken(authObj.getString("accessToken"));

            this.saveData();

            this.getSettings();

            if (getGcmToken().length() != 0) {

                setGcmToken(getGcmToken());
            }

            return true;

        } catch (JSONException e) {

            e.printStackTrace();
            return false;
        }
    }

    public long getId() {

        return this.id;
    }

    public void setId(long id) {

        this.id = id;
    }

    public void setGcmToken(final String gcmToken) {

        if (this.getId() != 0 && this.getAccessToken().length() != 0) {

            CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_ACCOUNT_SET_GCM_TOKEN, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                if (!response.getBoolean("error")) {

//                                    Toast.makeText(getApplicationContext(), response.toString(), Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {

                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.e("setGcmToken", error.toString());
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("accountId", Long.toString(App.getInstance().getId()));
                    params.put("accessToken", App.getInstance().getAccessToken());

                    params.put("fcm_regId", gcmToken);

                    return params;
                }
            };

            int socketTimeout = 0;//0 seconds - change to what you want
            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

            jsonReq.setRetryPolicy(policy);

            App.getInstance().addToRequestQueue(jsonReq);
        }

        this.gcmToken = gcmToken;
    }

    public String getGcmToken() {

        if (this.gcmToken == null) {

            this.gcmToken = "";
        }

        return this.gcmToken;
    }

    public void setFacebookId(String fb_id) {

        this.fb_id = fb_id;
    }

    public String getFacebookId() {

        return this.fb_id;
    }

    public void setState(int state) {

        this.state = state;
    }

    public int getState() {

        return this.state;
    }

    public void setNotificationsCount(int notificationsCount) {

        this.notificationsCount = notificationsCount;

        updateMainActivityBadges(this, "");
    }

    public int getNotificationsCount() {

        return this.notificationsCount;
    }

    public void setMessagesCount(int messagesCount) {

        this.messagesCount = messagesCount;

        updateMainActivityBadges(this, "");
    }

    public int getMessagesCount() {

        return this.messagesCount;
    }

    public void setGuestsCount(int guestsCount) {

        this.guestsCount = guestsCount;
    }

    public int getGuestsCount() {

        return this.guestsCount;
    }

    public void setNewFriendsCount(int newFriendsCount) {

        this.newFriendsCount = newFriendsCount;
    }

    public int getNewFriendsCount() {

        return this.newFriendsCount;
    }

    public void setAllowMessagesGCM(int allowMessagesGCM) {

        this.allowMessagesGCM = allowMessagesGCM;
    }

    public int getAllowMessagesGCM() {

        return this.allowMessagesGCM;
    }

    public void setAllowCommentReplyGCM(int allowCommentReplyGCM) {

        this.allowCommentReplyGCM = allowCommentReplyGCM;
    }

    public int getAllowCommentReplyGCM() {

        return this.allowCommentReplyGCM;
    }

    public void setAllowFollowersGCM(int allowFollowersGCM) {

        this.allowFollowersGCM = allowFollowersGCM;
    }

    public int getAllowFollowersGCM() {

        return this.allowFollowersGCM;
    }

    public void setAllowGiftsGCM(int allowGiftsGCM) {

        this.allowGiftsGCM = allowGiftsGCM;
    }

    public int getAllowGiftsGCM() {

        return this.allowGiftsGCM;
    }

    public void setAllowCommentsGCM(int allowCommentsGCM) {

        this.allowCommentsGCM = allowCommentsGCM;
    }

    public int getAllowCommentsGCM() {

        return this.allowCommentsGCM;
    }

    public void setAllowLikesGCM(int allowLikesGCM) {

        this.allowLikesGCM = allowLikesGCM;
    }

    public int getAllowLikesGCM() {

        return this.allowLikesGCM;
    }

    public void setAllowMessages(int allowMessages) {

        this.allowMessages = allowMessages;
    }

    public int getAllowMessages() {

        return this.allowMessages;
    }

    public void setAllowComments(int allowComments) {

        this.allowComments = allowComments;
    }

    public int getAllowComments() {

        return this.allowComments;
    }

    public void setAllowGalleryComments(int allowGalleryComments) {

        this.allowGalleryComments = allowGalleryComments;
    }

    public int getAllowGalleryComments() {

        return this.allowGalleryComments;
    }

    public void setAdmob(int admob) {

        this.admob = admob;
    }

    public int getAdmob() {

        return this.admob;
    }

    public void setAllowRewardedAds(int allowRewardedAds) {

        this.allowRewardedAds = allowRewardedAds;
    }

    public int getAllowRewardedAds() {

        return this.allowRewardedAds;
    }

    public void setGhost(int ghost) {

        this.ghost = ghost;
    }

    public int getGhost() {

        return this.ghost;
    }

    public void setPro(int pro) {

        this.pro = pro;
    }

    public int getPro() {

        return this.pro;
    }

    public Boolean isPro() {

        if (this.pro > 0) {

            return true;
        }

        return false;
    }

    public void setCurrentChatId(int currentChatId) {

        this.currentChatId = currentChatId;
    }

    public int getCurrentChatId() {

        return this.currentChatId;
    }

    public void setErrorCode(int errorCode) {

        this.errorCode = errorCode;
    }

    public int getErrorCode() {

        return this.errorCode;
    }

    public String getUsername() {

        if (this.username == null) {

            this.username = "";
        }

        return this.username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public String getAccessToken() {

        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    public void setFullname(String fullname) {

        this.fullname = fullname;
    }

    public String getFullname() {

        if (this.fullname == null) {

            this.fullname = "";
        }

        return this.fullname;
    }

    public void setVerify(int verify) {

        this.verify = verify;
    }

    public int getVerify() {

        return this.verify;
    }

    public void setBalance(int balance) {

        this.balance = balance;
    }

    public int getBalance() {

        return this.balance;
    }

    public void setPhotoUrl(String photoUrl) {

        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {

        if (this.photoUrl == null) {

            this.photoUrl = "";
        }

        return this.photoUrl;
    }

    public void setCoverUrl(String coverUrl) {

        this.coverUrl = coverUrl;
    }

    public String getCoverUrl() {

        if (coverUrl == null) {

            this.coverUrl = "";
        }

        return this.coverUrl;
    }

    public void setCountry(String country) {

        this.country = country;
    }

    public String getCountry() {

        if (this.country == null) {

            this.setCountry("");
        }

        return this.country;
    }

    public void setCity(String city) {

        this.city = city;
    }

    public String getCity() {

        if (this.city == null) {

            this.setCity("");
        }

        return this.city;
    }

    public void setArea(String area) {

        this.area = area;
    }

    public String getArea() {

        if (this.area == null) {

            this.setArea("");
        }

        return this.area;
    }

    public void setLat(Double lat) {

        if (this.lat == null) {

            this.lat = 0.000000;
        }

        this.lat = lat;
    }

    public Double getLat() {

        if (this.lat == null) {

            this.lat = 0.000000;
        }

        return this.lat;
    }

    public void setLng(Double lng) {

        if (this.lng == null) {

            this.lng = 0.000000;
        }

        this.lng = lng;
    }

    public Double getLng() {

        return this.lng;
    }

    // Privacy

    public void setAllowShowMyInfo(int allowShowMyInfo) {

        this.allowShowMyInfo = allowShowMyInfo;
    }

    public int getAllowShowMyInfo() {

        return this.allowShowMyInfo;
    }

    public void setAllowShowMyFriends(int allowShowMyFriends) {

        this.allowShowMyFriends = allowShowMyFriends;
    }

    public int getAllowShowMyFriends() {

        return this.allowShowMyFriends;
    }

    public void setAllowShowMyGallery(int allowShowMyGallery) {

        this.allowShowMyGallery = allowShowMyGallery;
    }

    public int getAllowShowMyGallery() {

        return this.allowShowMyGallery;
    }

    public void setAllowShowMyGifts(int allowShowMyGifts) {

        this.allowShowMyGifts = allowShowMyGifts;
    }

    public int getAllowShowMyGifts() {

        return this.allowShowMyGifts;
    }

    public ArrayList<Feeling> getFeelingsList() {

	    if (this.feelingsList == null) {

            feelingsList = new ArrayList<Feeling>();
        }

        return this.feelingsList;
    }

    public ArrayList<BaseGift> getGiftsList() {

        if (this.giftsList == null) {

            giftsList = new ArrayList<BaseGift>();
        }

        return this.giftsList;
    }

    public void readData() {

        this.setId(sharedPref.getLong(getString(R.string.settings_account_id), 0));
        this.setUsername(sharedPref.getString(getString(R.string.settings_account_username), ""));
        this.setAccessToken(sharedPref.getString(getString(R.string.settings_account_access_token), ""));

        this.setAllowMessagesGCM(sharedPref.getInt(getString(R.string.settings_account_allow_messages_gcm), 1));
        this.setAllowCommentsGCM(sharedPref.getInt(getString(R.string.settings_account_allow_comments_gcm), 1));
        this.setAllowCommentReplyGCM(sharedPref.getInt(getString(R.string.settings_account_allow_comments_reply_gcm), 1));

        this.setBalance(sharedPref.getInt(getString(R.string.settings_balance), 0));
        this.setVerify(sharedPref.getInt(getString(R.string.settings_verified_barge), 0));
        this.setGhost(sharedPref.getInt(getString(R.string.settings_ghost_mode), 0));
        this.setAdmob(sharedPref.getInt(getString(R.string.settings_ads_mode), 0));

        this.setPro(sharedPref.getInt(getString(R.string.settings_pro_mode), 0));

        this.setPhotoUrl(sharedPref.getString(getString(R.string.settings_account_photo_url), ""));
        this.setFullname(sharedPref.getString(getString(R.string.settings_account_fullname), ""));

        this.setLat(Double.parseDouble(sharedPref.getString(getString(R.string.settings_account_lat), "0.000000")));
        this.setLng(Double.parseDouble(sharedPref.getString(getString(R.string.settings_account_lng), "0.000000")));

        this.setAllowGiftsGCM(sharedPref.getInt(getString(R.string.settings_account_allow_gifts_gcm), 1));
        this.setAllowLikesGCM(sharedPref.getInt(getString(R.string.settings_account_allow_likes_gcm), 1));
        this.setAllowFollowersGCM(sharedPref.getInt(getString(R.string.settings_account_allow_friends_requests_gcm), 1));
    }

    public void saveData() {

        sharedPref.edit().putLong(getString(R.string.settings_account_id), this.getId()).apply();
        sharedPref.edit().putString(getString(R.string.settings_account_username), this.getUsername()).apply();
        sharedPref.edit().putString(getString(R.string.settings_account_access_token), this.getAccessToken()).apply();

        sharedPref.edit().putInt(getString(R.string.settings_account_allow_messages_gcm), this.getAllowMessagesGCM()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_account_allow_comments_gcm), this.getAllowCommentsGCM()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_account_allow_comments_reply_gcm), this.getAllowCommentReplyGCM()).apply();

        sharedPref.edit().putInt(getString(R.string.settings_balance), this.getBalance()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_verified_barge), this.getVerify()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_ghost_mode), this.getGhost()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_ads_mode), this.getAdmob()).apply();

        sharedPref.edit().putInt(getString(R.string.settings_pro_mode), this.getPro()).apply();

        sharedPref.edit().putString(getString(R.string.settings_account_photo_url), this.getPhotoUrl()).apply();
        sharedPref.edit().putString(getString(R.string.settings_account_fullname), this.getFullname()).apply();

        sharedPref.edit().putString(getString(R.string.settings_account_lat), Double.toString(this.getLat())).apply();
        sharedPref.edit().putString(getString(R.string.settings_account_lng), Double.toString(this.getLng())).apply();

        sharedPref.edit().putInt(getString(R.string.settings_account_allow_gifts_gcm), this.getAllowGiftsGCM()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_account_allow_likes_gcm), this.getAllowLikesGCM()).apply();
        sharedPref.edit().putInt(getString(R.string.settings_account_allow_friends_requests_gcm), this.getAllowFollowersGCM()).apply();
    }

    public void removeData() {

        sharedPref.edit().putLong(getString(R.string.settings_account_id), 0).apply();
        sharedPref.edit().putString(getString(R.string.settings_account_username), "").apply();
        sharedPref.edit().putString(getString(R.string.settings_account_access_token), "").apply();

        sharedPref.edit().putInt(getString(R.string.settings_balance), 0).apply();
        sharedPref.edit().putInt(getString(R.string.settings_verified_barge), 0).apply();
        sharedPref.edit().putInt(getString(R.string.settings_ghost_mode), 0).apply();
        sharedPref.edit().putInt(getString(R.string.settings_ads_mode), 0).apply();
        sharedPref.edit().putInt(getString(R.string.settings_pro_mode), 0).apply();

        sharedPref.edit().putString(getString(R.string.settings_account_photo_url), "").apply();
        sharedPref.edit().putString(getString(R.string.settings_account_fullname), "").apply();

        sharedPref.edit().putString(getString(R.string.settings_account_lat), "0.000000").apply();
        sharedPref.edit().putString(getString(R.string.settings_account_lng), "0.000000").apply();
    }

    public static void updateMainActivityBadges(Context context, String message) {

        Intent intent = new Intent(TAG_UPDATE_BADGES);
        intent.putExtra("message", message); // if need message
        context.sendBroadcast(intent);
    }

    public static synchronized App getInstance() {
		return mInstance;
	}

	public RequestQueue getRequestQueue() {

		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(getApplicationContext());
		}

		return mRequestQueue;
	}

	public ImageLoader getImageLoader() {
		getRequestQueue();
		if (mImageLoader == null) {
			mImageLoader = new ImageLoader(this.mRequestQueue,
					new LruBitmapCache());
		}
		return this.mImageLoader;
	}

	public <T> void addToRequestQueue(Request<T> req, String tag) {
		// set the default tag if tag is empty
		req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
		getRequestQueue().add(req);
	}

	public <T> void addToRequestQueue(Request<T> req) {
		req.setTag(TAG);
		getRequestQueue().add(req);
	}

	public void cancelPendingRequests(Object tag) {
		if (mRequestQueue != null) {
			mRequestQueue.cancelAll(tag);
		}
	}
}