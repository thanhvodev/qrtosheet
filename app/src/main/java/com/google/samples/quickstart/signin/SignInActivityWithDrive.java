package com.google.samples.quickstart.signin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.zxing.Result;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile, which also adds a request dialog to access the user's Google Drive.
 */
public class SignInActivityWithDrive extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private CodeScanner mCodeScanner;
    boolean CameraPermission = false;
    final int CAMERA_PERM = 1;
    private GoogleSignInClient mGoogleSignInClient;

    private String accessToken = "";
    private String spreadsheetID = "";
    private final static String API_KEY = "AIzaSyCE2B_tzd_72dOds0bZwl5o6qwS0NqIOlY";
    private String userName = "";
    private EditText spreadID;
    private SharedPreferences sp;
    private int currentDay;
    private int lastDay;

    private TextView email;
    private FrameLayout frameLayout;
    private Button sign_out_button;
    private SignInButton signInButton;
    private TextView mStatusTextView;

    private Handler mHandle = new Handler();

    private String authCode;
    private boolean alreadyExecuted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email = findViewById(R.id.email);
        frameLayout = findViewById(R.id.frameLayout);
        signInButton = findViewById(R.id.sign_in_button);
        sign_out_button = findViewById(R.id.sign_out_button);
        mStatusTextView = findViewById(R.id.status);

        spreadID = findViewById(R.id.editText);
        sp = getSharedPreferences("localStorage", Context.MODE_PRIVATE);
        spreadID.setText(sp.getString("SPREADSHEETID", ""));

        Calendar calendar = Calendar.getInstance();
        currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        lastDay = sp.getInt("day", Context.MODE_PRIVATE);

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        askPermission();
        if (CameraPermission) {
            scannerView.setOnClickListener(view -> mCodeScanner.startPreview());

            mCodeScanner.setDecodeCallback(this::onDecoded);
        }
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // Views
        // Button listeners
        signInButton.setOnClickListener(this);
        sign_out_button.setOnClickListener(this);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope("https://www.googleapis.com/auth/spreadsheets"))
                .requestEmail()
                .requestServerAuthCode(getString(R.string.server_client_id))
                .requestIdToken(getString(R.string.server_client_id))
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // [END build_client]

        // [START customize_button]
        // Customize sign-in button. The sign-in button can be displayed in
        // multiple sizes.

        signInButton.setSize(SignInButton.SIZE_STANDARD);
        // [END customize_button]
    }

    private Runnable mGetToken = new Runnable() {
        @Override
        public void run() {
            signIn();
        }
    };

    private void makeSheetOnceADay() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("day", currentDay);
        editor.apply();
        try {
            createSheetPerDay();
            addSheetHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope("https://www.googleapis.com/auth/spreadsheets"))) {
            updateUI(account);
            signIn();
        } else {
            updateUI(null);
        }

    }

    private void askPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SignInActivityWithDrive.this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERM);
            } else {
                mCodeScanner.startPreview();
                CameraPermission = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCodeScanner.startPreview();
                CameraPermission = true;
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this).setTitle("Permission").setMessage("Please provide the camera permission for using all the features of the app")
                            .setPositiveButton("Proceed", (dialogInterface, i) -> ActivityCompat.requestPermissions(SignInActivityWithDrive.this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERM)).setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss()).create().show();
                } else {
                    new AlertDialog.Builder(this).setTitle("Permission").setMessage("You have denied some permission. Allow all permission at [Settings] > [Permissions]")
                            .setPositiveButton("Settings", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();

                            }).setNegativeButton("No, Exit app", (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                finish();
                            }).create().show();
                }
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        if (CameraPermission) {
            mCodeScanner.releaseResources();
        }
        super.onPause();

    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(@Nullable Task<GoogleSignInAccount> completedTask) {
        try {
            // Signed in successfully, show authenticated U
            assert completedTask != null;
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            updateUI(account);
        } catch (ApiException e) {
            // Signed out, show unauthenticated UI.
            Log.w(TAG, "handleSignInResult:error", e);
            updateUI(null);
        }
    }
    // [END handleSignInResult]

    // [START signIn]
    private void signIn() {

        spreadsheetID = spreadID.getText().toString();
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("SPREADSHEETID", spreadsheetID);
        editor.apply();

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // [START_EXCLUDE]
            updateUI(null);
            // [END_EXCLUDE]
        });
        mHandle.removeCallbacks(mGetToken);
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                task -> {
                    // [START_EXCLUDE]
                    updateUI(null);
                    // [END_EXCLUDE]
                });
    }
    // [END revokeAccess]

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            if (!sp.getBoolean("alreadyExecuted", false)) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean("alreadyExecuted", true);
                editor.apply();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            authCode = account.getServerAuthCode();
            userName = account.getDisplayName();
            email.setText(account.getEmail());
            email.setVisibility(View.VISIBLE);
            spreadID.setVisibility(View.GONE);
            mStatusTextView.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            sign_out_button.setVisibility(View.VISIBLE);
            new Thread(this::getAToken).start();
            mHandle.postDelayed(mGetToken, 3_000_000);
        } else {
            email.setVisibility(View.GONE);
            spreadID.setVisibility(View.VISIBLE);
            mStatusTextView.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.VISIBLE);
            sign_out_button.setVisibility(View.GONE);
            frameLayout.setVisibility(View.GONE);
        }



    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                revokeAccess();
                break;
        }
    }

    private void getAToken() {
        try {
            GoogleTokenResponse tokenResponse =
                    new GoogleAuthorizationCodeTokenRequest(
                            new NetHttpTransport(),
                            JacksonFactory.getDefaultInstance(),
                            "https://accounts.google.com/o/oauth2/token",
                            getString(R.string.client_id),
                            getString(R.string.client_secret),
                            authCode,
                            "")
                            .execute();
            accessToken = tokenResponse.getAccessToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("Token", accessToken);
    }

    @SuppressLint("NewApi")
    private static String getTime() {
        return String.valueOf(Calendar.getInstance().getTime());
    }

    @SuppressLint("NewApi")
    private static String getDate() {
        return String.valueOf(LocalDate.now());
    }

    private void createSheetPerDay() throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"requests\": [\r\n    {\r\n      \"addSheet\": {\r\n        \"properties\": {\r\n          \"title\": \"" + getDate() + "\",\r\n          \"gridProperties\": {\r\n            \"rowCount\": 20,\r\n            \"columnCount\": 12\r\n          },\r\n        }\r\n      }\r\n    }\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/" + spreadsheetID +":batchUpdate")
                .method("POST", body)
                .addHeader("Authorization", "Bearer " +  accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        try {
            Response response = client.newCall(request).execute();
        } catch (Exception e) {

        }
    }

    private void addSheetHeader() {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \"Thời gian quét\",\r\n      \"Người quét\",\r\n      \"QR Code\",\r\n    ],\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/"+ spreadsheetID +"/values/" + getDate() + "!A1%3AC1?includeValuesInResponse=true&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=" + API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer "+accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        try {
            Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendSheet(String result) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \"" + getTime() + "\",\r\n      \""+ userName +"\",\r\n      \"" + result + "\"\r\n    ]\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/"+ spreadsheetID +"/values/" +getDate()+ ":append?includeValuesInResponse=true&insertDataOption=INSERT_ROWS&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=" + API_KEY)
                                .method("POST", body)
                                .addHeader("Authorization", "Bearer " + accessToken)
                                .addHeader("Content-Type", "text/plain")
                                .build();
        Response response = client.newCall(request).execute();
    }

    private void onDecoded(Result result) {

        runOnUiThread(() -> {
            Intent intent = getIntent();
            finish();
            startActivity(intent);

            Toast.makeText(SignInActivityWithDrive.this, result.getText(), Toast.LENGTH_LONG).show();
            new Thread(() -> {
                if (lastDay != currentDay) {
                    makeSheetOnceADay();
                }
                try {
                    appendSheet(result.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
