package ru.ifsoft.network;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.balysv.materialripple.MaterialRippleLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import github.ankushsachdeva.emojicon.EditTextImeBackListener;
import github.ankushsachdeva.emojicon.EmojiconEditText;
import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;
import ru.ifsoft.network.adapter.FeelingsListAdapter;
import ru.ifsoft.network.adapter.MediaListAdapter;
import ru.ifsoft.network.app.App;
import ru.ifsoft.network.constants.Constants;
import ru.ifsoft.network.model.Feeling;
import ru.ifsoft.network.model.GalleryItem;
import ru.ifsoft.network.model.Image;
import ru.ifsoft.network.model.Item;
import ru.ifsoft.network.model.MediaItem;
import ru.ifsoft.network.util.Api;
import ru.ifsoft.network.util.CustomRequest;
import ru.ifsoft.network.util.Helper;

public class NewGalleryItemFragment extends Fragment implements Constants {

    private static final int VIDEO_FILES_LIMIT = 1;
    private static final int IMAGE_FILES_LIMIT = 7;

    public static final int REQUEST_TAKE_GALLERY_VIDEO = 1001;

    private static final String STATE_LIST = "State Adapter Data";

    public static final int RESULT_OK = -1;

    private static final int ITEM_FEELINGS = 1;

    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;

    private MaterialRippleLayout mOpenBottomSheet;

    private BottomSheetBehavior mBehavior;
    private BottomSheetDialog mBottomSheetDialog;
    private View mBottomSheet;

    private CircularImageView mPhoto;
    private TextView mFullname;

    private LinearLayout mMediaContainer, mDeleteMedia;
    private ImageView mThumbnail, mPlayIcon;
    private ProgressBar mProgressBar;

    private ProgressDialog pDialog;

    EmojiconEditText mPostEdit;
    ImageView mEmojiBtn;

    private GalleryItem item;

    private String selectedPostImg = "";
    private String selectedPostVideo = "";

    private Boolean loading = false;

    EmojiconsPopup popup;

    public NewGalleryItemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        setHasOptionsMenu(true);

        initpDialog();

        Intent i = getActivity().getIntent();

        if (i.getExtras() != null) {

            item = (GalleryItem) i.getExtras().getParcelable("item");

            if (item == null) {

                item = new GalleryItem();
            }

        } else {

            item = new GalleryItem();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_gallery_item, container, false);

        if (savedInstanceState != null) {

            item = savedInstanceState.getParcelable("item");
        }

        popup = new EmojiconsPopup(rootView, getActivity());

        popup.setSizeForSoftKeyboard();

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {

                mPostEdit.append(emojicon.getEmoji());
            }
        });

        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {

                KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mPostEdit.dispatchKeyEvent(event);
            }
        });

        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {

                setIconEmojiKeyboard();
            }
        });

        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {

                if (popup.isShowing())

                    popup.dismiss();
            }
        });

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {

                mPostEdit.append(emojicon.getEmoji());
            }
        });

        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {

                KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mPostEdit.dispatchKeyEvent(event);
            }
        });

        if (loading) {

            showpDialog();
        }

        //

        mMediaContainer = (LinearLayout) rootView.findViewById(R.id.media_container);
        mDeleteMedia = (LinearLayout) rootView.findViewById(R.id.delete);

        mPlayIcon = (ImageView) rootView.findViewById(R.id.play_icon);
        mThumbnail = (ImageView) rootView.findViewById(R.id.thumbnail);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        mDeleteMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectedPostImg = "";
                selectedPostVideo = "";

                updateMediaContainer();
            }
        });

        //

        mOpenBottomSheet = (MaterialRippleLayout) rootView.findViewById(R.id.open_bottom_sheet_button);

        mOpenBottomSheet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                showBottomSheet();
            }
        });

        // Prepare bottom sheet

        mBottomSheet = rootView.findViewById(R.id.bottom_sheet);
        mBehavior = BottomSheetBehavior.from(mBottomSheet);

        //

        mPhoto = (CircularImageView) rootView.findViewById(R.id.photo_image);
        mFullname = (TextView) rootView.findViewById(R.id.fullname_label);

        //


        mEmojiBtn = (ImageView) rootView.findViewById(R.id.emojiBtn);
        mEmojiBtn.setVisibility(View.GONE);

        mPostEdit = (EmojiconEditText) rootView.findViewById(R.id.postEdit);
        mPostEdit.setText(item.getComment());

        mPostEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (isAdded()) {

                    if (hasFocus) {

                        //got focus

                        if (EMOJI_KEYBOARD) {

                            mEmojiBtn.setVisibility(View.VISIBLE);
                        }

                    } else {

                        mEmojiBtn.setVisibility(View.GONE);
                    }
                }
            }
        });

        setEditTextMaxLength(POST_CHARACTERS_LIMIT);

        mPostEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                int cnt = s.length();

                if (cnt == 0) {

                    updateTitle();

                } else {

                    getActivity().setTitle(Integer.toString(POST_CHARACTERS_LIMIT - cnt));
                }
            }

        });

        mEmojiBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!popup.isShowing()) {

                    if (popup.isKeyBoardOpen()){

                        popup.showAtBottom();
                        setIconSoftKeyboard();

                    } else {

                        mPostEdit.setFocusableInTouchMode(true);
                        mPostEdit.requestFocus();
                        popup.showAtBottomPending();

                        final InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(mPostEdit, InputMethodManager.SHOW_IMPLICIT);
                        setIconSoftKeyboard();
                    }

                } else {

                    popup.dismiss();
                }
            }
        });

        EditTextImeBackListener er = new EditTextImeBackListener() {

            @Override
            public void onImeBack(EmojiconEditText ctrl, String text) {

                hideEmojiKeyboard();
            }
        };

        mPostEdit.setOnEditTextImeBackListener(er);

        updateMediaContainer();
        updateTitle();
        updateProfileInfo();

        // Inflate the layout for this fragment
        return rootView;
    }

    private void updateMediaContainer() {

        mMediaContainer.setVisibility(View.GONE);
        mPlayIcon.setVisibility(View.GONE);
        mOpenBottomSheet.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);

        if (selectedPostImg != null && selectedPostImg.length() > 0) {

            mOpenBottomSheet.setVisibility(View.GONE);

            mMediaContainer.setVisibility(View.VISIBLE);

            mThumbnail.setImageURI(FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", new File(selectedPostImg)));

            if (selectedPostVideo.length() >  0) {

                mPlayIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateTitle() {

        if (isAdded()) {

            if (item.getId() != 0) {

                getActivity().setTitle(getText(R.string.title_edit_item));

            } else {

                getActivity().setTitle(getText(R.string.title_new_item));
            }
        }
    }

    private void updateProfileInfo() {

        if (isAdded()) {

            if (App.getInstance().getPhotoUrl() != null && App.getInstance().getPhotoUrl().length() > 0) {

                App.getInstance().getImageLoader().get(App.getInstance().getPhotoUrl(), ImageLoader.getImageListener(mPhoto, R.drawable.profile_default_photo, R.drawable.profile_default_photo));

            } else {

                mPhoto.setImageResource(R.drawable.profile_default_photo);
            }

            mFullname.setText(App.getInstance().getFullname());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {

                // If request is cancelled, the result arrays are empty.

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Check GPS is enabled
                    LocationManager lm = (LocationManager) getActivity().getSystemService(getActivity().LOCATION_SERVICE);

                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

                        mFusedLocationClient.getLastLocation().addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {

                                if (task.isSuccessful() && task.getResult() != null) {

                                    mLastLocation = task.getResult();

                                    // Set geo data to App class

                                    App.getInstance().setLat(mLastLocation.getLatitude());
                                    App.getInstance().setLng(mLastLocation.getLongitude());

                                    // Save data

                                    App.getInstance().saveData();

                                    // Get address

                                    App.getInstance().getAddress(App.getInstance().getLat(), App.getInstance().getLng());

                                } else {

                                    Log.d("GPS", "New Item getLastLocation:exception", task.getException());
                                }
                            }
                        });
                    }

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) || !ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {

                        showNoLocationPermissionSnackbar();
                    }
                }

                return;
            }

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_PHOTO: {

                // If request is cancelled, the result arrays are empty.

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    choiceImageAction();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        showNoStoragePermissionSnackbar();
                    }
                }

                return;
            }

            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_VIDEO_IMAGE: {

                // If request is cancelled, the result arrays are empty.

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    choiceVideo();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

                    if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        showNoStoragePermissionSnackbar();
                    }
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void showNoStoragePermissionSnackbar() {

        Snackbar.make(getView(), getString(R.string.label_no_storage_permission) , Snackbar.LENGTH_LONG).setAction(getString(R.string.action_settings), new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                openApplicationSettings();

                Toast.makeText(getActivity(), getString(R.string.label_grant_storage_permission), Toast.LENGTH_SHORT).show();
            }

        }).show();
    }

    public void showNoLocationPermissionSnackbar() {

        Snackbar.make(getView(), getString(R.string.label_no_location_permission) , Snackbar.LENGTH_LONG).setAction(getString(R.string.action_settings), new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                openApplicationSettings();

                Toast.makeText(getActivity(), getString(R.string.label_grant_location_permission), Toast.LENGTH_SHORT).show();
            }

        }).show();
    }

    public void openApplicationSettings() {

        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getActivity().getPackageName()));
        startActivityForResult(appSettingsIntent, 10001);
    }

    public void setEditTextMaxLength(int length) {

        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(length);
        mPostEdit.setFilters(FilterArray);
    }

    public void hideEmojiKeyboard() {

        popup.dismiss();
    }

    public void setIconEmojiKeyboard() {

        mEmojiBtn.setBackgroundResource(R.drawable.ic_emoji);
    }

    public void setIconSoftKeyboard() {

        mEmojiBtn.setBackgroundResource(R.drawable.ic_keyboard);
    }

    public void onDestroyView() {

        super.onDestroyView();

        hidepDialog();
    }

    protected void initpDialog() {

        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage(getString(R.string.msg_loading));
        pDialog.setCancelable(false);
    }

    protected void showpDialog() {

        if (!pDialog.isShowing()) pDialog.show();
    }

    protected void hidepDialog() {

        if (pDialog.isShowing()) pDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putParcelable("item", item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mItem) {

        switch (mItem.getItemId()) {

            case R.id.action_post: {

                hideEmojiKeyboard();

                this.item.setComment(mPostEdit.getText().toString().trim());

                if (selectedPostVideo.length() == 0 && selectedPostImg.length() == 0) {

                    Toast toast= Toast.makeText(getActivity(), getText(R.string.msg_enter_item), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                } else {

                    loading = true;

                    showpDialog();

                    if (selectedPostVideo.length() == 0) {

                        File f = new File(selectedPostImg);

                        uploadFile(f);

                    } else {

                        File f = new File(selectedPostImg);

                        File f2 = new File(selectedPostVideo);

                        uploadVideoFile(f, f2);
                    }

                    newPost();
                }

                return true;
            }

            default: {

                break;
            }
        }

        return false;
    }

    public Bitmap resizeBitmap(String photoPath) {

        Log.e("Image", "resizeBitmap()");

        int targetW = 512;
        int targetH = 512;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;

        scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true; //Deprecated from  API 21

        return BitmapFactory.decodeFile(photoPath, bmOptions);
    }

    public Boolean save(String outFile, String inFile) {

        Boolean status = true;

        try {

            Bitmap bmp = resizeBitmap(outFile);

            File file = new File(Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER, inFile);
            FileOutputStream fOut = new FileOutputStream(file);

            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
            fOut.flush();
            fOut.close();

        } catch (Exception ex) {

            status = false;

            Log.e("Error", ex.getMessage());
        }

        return status;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_POST_IMG && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();

            String newFileName = Helper.randomString(6) + ".jpg";

            selectedPostImg = getImageUrlWithAuthority(getActivity(), selectedImage, newFileName);

            try {

                if (save(selectedPostImg, newFileName)) {

                    selectedPostImg = Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER + File.separator + newFileName;

                } else {

                    selectedPostImg = "";
                }

            } catch (Exception e) {

                selectedPostImg = "";

                Log.e("OnSelectPostImage", e.getMessage());
            }

            updateMediaContainer();

        } else if (requestCode == CREATE_POST_IMG && resultCode == getActivity().RESULT_OK) {

            try {

                selectedPostImg = Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER + File.separator + "camera.jpg";

                String newFileName = Helper.randomString(6) + ".jpg";

                save(selectedPostImg, newFileName);

                selectedPostImg = Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER + File.separator + newFileName;

            } catch (Exception ex) {

                selectedPostImg = "";

                Log.v("OnCameraCallBack", ex.getMessage());
            }

            updateMediaContainer();

        } else if (requestCode == REQUEST_TAKE_GALLERY_VIDEO && resultCode == getActivity().RESULT_OK) {

            Uri selectedVideoUri = data.getData();

            selectedPostVideo = getRealPathFromURI(selectedVideoUri);

            File videoFile = new File(selectedPostVideo);

            if (videoFile.length() > VIDEO_FILE_MAX_SIZE) {

                Toast.makeText(getActivity(), getString(R.string.msg_video_too_large), Toast.LENGTH_SHORT).show();

            } else {

                if (selectedPostVideo != null) {

                    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(selectedPostVideo, MediaStore.Images.Thumbnails.MINI_KIND);
                    Matrix matrix = new Matrix();
                    Bitmap bmThumbnail = Bitmap.createBitmap(thumb, 0, 0, thumb.getWidth(), thumb.getHeight(), matrix, true);

                    String newFileName = Helper.randomString(6) + ".jpg";

                    selectedPostImg = writeToTempImageAndGetPathUri(getActivity(), bmThumbnail, newFileName);

                    selectedPostImg = Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER + File.separator + newFileName;
                }
            }

            updateMediaContainer();
        }
    }

    public static String getImageUrlWithAuthority(Context context, Uri uri, String fileName) {

        InputStream is = null;

        if (uri.getAuthority() != null) {

            try {

                is = context.getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);

                return writeToTempImageAndGetPathUri(context, bmp, fileName).toString();

            } catch (FileNotFoundException e) {

                e.printStackTrace();

            } finally {

                try {

                    if (is != null) {

                        is.close();
                    }

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public String getRealPathFromURI(Uri contentUri) {

        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(contentUri, proj, null, null, null);

        if (cursor.moveToFirst()) {

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }

        cursor.close();

        return res;
    }

    public static String writeToTempImageAndGetPathUri(Context inContext, Bitmap inImage, String fileName) {

        String file_path = Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER;
        File dir = new File(file_path);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);

        try {

            FileOutputStream fos = new FileOutputStream(file);

            inImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

        } catch (FileNotFoundException e) {

            Toast.makeText(inContext, "Error occured. Please try again later.", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {

            e.printStackTrace();
        }

        return Environment.getExternalStorageDirectory() + File.separator + APP_TEMP_FOLDER + File.separator + fileName;
    }

    public void choiceVideo() {

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, getActivity().getString(R.string.label_select_video)), REQUEST_TAKE_GALLERY_VIDEO);
    }

    public void choiceImageAction() {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        arrayAdapter.add(getText(R.string.action_gallery).toString());
        arrayAdapter.add(getText(R.string.action_camera).toString());

        builderSingle.setTitle(getText(R.string.dlg_choice_image_title));


        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {

                    case 0: {

                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(Intent.createChooser(intent, getText(R.string.label_select_img)), SELECT_POST_IMG);

                        break;
                    }

                    default: {

                        try {

                            File root = new File(Environment.getExternalStorageDirectory(), APP_TEMP_FOLDER);

                            if (!root.exists()) {

                                root.mkdirs();
                            }

                            File sdImageMainDirectory = new File(root, "camera.jpg");

                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", sdImageMainDirectory));
                            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivityForResult(cameraIntent, CREATE_POST_IMG);

                        } catch (Exception e) {

                            Log.e("Camera", "Error occured. Please try again later.");
                        }

                        break;
                    }
                }
            }
        });

        builderSingle.setNegativeButton(getText(R.string.action_cancel), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        AlertDialog d = builderSingle.create();
        d.show();
    }

    private void newPost() {

        CustomRequest jsonReq = new CustomRequest(Request.Method.POST, METHOD_GALLERY_ITEM_NEW, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            if (!response.getBoolean("error")) {


                            }

                        } catch (JSONException e) {

                            e.printStackTrace();

                        } finally {

                            sendPostSuccess();

                            Log.e("Response", response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                sendPostSuccess();

//                     Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("accountId", Long.toString(App.getInstance().getId()));
                params.put("accessToken", App.getInstance().getAccessToken());
                params.put("comment", item.getComment());
                params.put("originImgUrl", item.getOriginImgUrl());
                params.put("previewImgUrl", item.getPreviewImgUrl());
                params.put("imgUrl", item.getImgUrl());
                params.put("postArea", item.getArea());
                params.put("postCountry", item.getCountry());
                params.put("postCity", item.getCity());
                params.put("postLat", Double.toString(item.getLat()));
                params.put("postLng", Double.toString(item.getLng()));

                params.put("previewVideoImgUrl", item.getPreviewVideoImgUrl());
                params.put("videoUrl", item.getVideoUrl());

                return params;
            }
        };

        int socketTimeout = 0;//0 seconds - change to what you want
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonReq.setRetryPolicy(policy);

        App.getInstance().addToRequestQueue(jsonReq);
    }

    public void sendPostSuccess() {

        loading = false;

        hidepDialog();

        if (isAdded()) {

            Intent i = new Intent();
            getActivity().setResult(RESULT_OK, i);

            Toast.makeText(getActivity(), getText(R.string.msg_item_posted), Toast.LENGTH_SHORT).show();

            getActivity().finish();
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
    }

    @Override
    public void onDetach() {

        super.onDetach();
    }


    private void showBottomSheet() {

        if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {

            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        if (App.getInstance().getCountry().length() == 0 && App.getInstance().getCity().length() == 0) {

            if (App.getInstance().getLat() != 0.000000 && App.getInstance().getLng() != 0.000000) {

                App.getInstance().getAddress(App.getInstance().getLat(), App.getInstance().getLng());
            }
        }

        final View view = getLayoutInflater().inflate(R.layout.item_gallery_editor_sheet_list, null);

        MaterialRippleLayout mAddImageButton = (MaterialRippleLayout) view.findViewById(R.id.add_image_button);
        MaterialRippleLayout mAddVideoButton = (MaterialRippleLayout) view.findViewById(R.id.add_video_button);

        mAddImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mBottomSheetDialog != null) {

                    mBottomSheetDialog.dismiss();
                }

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_PHOTO);

                    } else {

                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_PHOTO);
                    }

                } else {

                    choiceImageAction();
                }
            }
        });

        mAddVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mBottomSheetDialog != null) {

                    mBottomSheetDialog.dismiss();
                }

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_VIDEO_IMAGE);

                    } else {

                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_VIDEO_IMAGE);
                    }

                } else {

                    choiceVideo();
                }
            }
        });

        mBottomSheetDialog = new BottomSheetDialog(getActivity());

        mBottomSheetDialog.setContentView(view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mBottomSheetDialog.show();

        doKeepDialog(mBottomSheetDialog);

        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {

                mBottomSheetDialog = null;
            }
        });
    }

    // Prevent dialog dismiss when orientation changes
    private static void doKeepDialog(Dialog dialog){

        WindowManager.LayoutParams lp = new  WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);
    }

    public Boolean uploadFile( File file) {

        final OkHttpClient client = new OkHttpClient();

        client.setProtocols(Arrays.asList(Protocol.HTTP_1_1));

        try {

            RequestBody requestBody = new MultipartBuilder()
                    .type(MultipartBuilder.FORM)
                    .addFormDataPart("uploaded_file", file.getName(), RequestBody.create(MediaType.parse("text/csv"), file))
                    .addFormDataPart("accountId", Long.toString(App.getInstance().getId()))
                    .addFormDataPart("accessToken", App.getInstance().getAccessToken())
                    .build();

            com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                    .url(METHOD_GALLERY_UPLOAD_IMAGE)
                    .addHeader("Accept", "application/json;")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(com.squareup.okhttp.Request request, IOException e) {

                    loading = false;

                    hidepDialog();

                    Log.e("failure", request.toString());
                }

                @Override
                public void onResponse(com.squareup.okhttp.Response response) throws IOException {

                    String jsonData = response.body().string();

                    try {

                        JSONObject result = new JSONObject(jsonData);

                        if (!result.getBoolean("error")) {

                            item.setImgUrl(result.getString("normalPhotoUrl"));
                            item.setOriginImgUrl(result.getString("originPhotoUrl"));
                            item.setPreviewImgUrl(result.getString("previewPhotoUrl"));

                        }

                        Log.d("My App", response.toString());

                    } catch (Throwable t) {

                        Log.e("My App", "Could not parse malformed JSON: \"" + t.getMessage() + "\"");

                    } finally {

                        Log.e("response", jsonData);

                        newPost();
                    }
                }
            });

            return true;

        } catch (Exception ex) {
            // Handle the error

            loading = false;

            hidepDialog();
        }

        return false;
    }

    public Boolean uploadVideoFile(File file, File videoFile) {

        final OkHttpClient client = new OkHttpClient();

        client.setProtocols(Arrays.asList(Protocol.HTTP_1_1));

        try {

            RequestBody requestBody = new MultipartBuilder()
                    .type(MultipartBuilder.FORM)
                    .addFormDataPart("uploaded_file", file.getName(), RequestBody.create(MediaType.parse("text/csv"), file))
                    .addFormDataPart("uploaded_video_file", videoFile.getName(), RequestBody.create(MediaType.parse("text/csv"), videoFile))
                    .addFormDataPart("accountId", Long.toString(App.getInstance().getId()))
                    .addFormDataPart("accessToken", App.getInstance().getAccessToken())
                    .build();

            com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                    .url(METHOD_GALLERY_UPLOAD_VIDEO)
                    .addHeader("Accept", "application/json;")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(com.squareup.okhttp.Request request, IOException e) {

                    loading = false;

                    hidepDialog();

                    Log.e("failure", request.toString());
                }

                @Override
                public void onResponse(com.squareup.okhttp.Response response) throws IOException {

                    String jsonData = response.body().string();

                    Log.e("response", jsonData);

                    try {

                        JSONObject result = new JSONObject(jsonData);

                        if (!result.getBoolean("error")) {

                            item.setVideoUrl(result.getString("videoFileUrl"));
                            item.setPreviewVideoImgUrl(result.getString("imgFileUrl"));
                        }

                        Log.d("My App", response.toString());

                    } catch (Throwable t) {

                        Log.e("My App", "Could not parse malformed JSON: \"" + t.getMessage() + "\"");

                    } finally {

                        Log.e("response", jsonData);

                        newPost();
                    }

                }
            });

            return true;

        } catch (Exception ex) {
            // Handle the error

            loading = false;

            hidepDialog();
        }

        return false;
    }
}