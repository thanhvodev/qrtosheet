package com.google.samples.quickstart.signin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
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
import android.widget.FrameLayout;
import android.widget.TextView;

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
import com.google.samples.quickstart.signin.databinding.ActivityMainBinding;
import com.google.zxing.Result;


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

    private String accessToken;
    private String spreadsheetID;
    private String userName;
    private SharedPreferences sp;

    private TextView email;
    private FrameLayout frameLayout;
    private Button sign_out_button;
    private SignInButton signInButton;
    private TextView mStatusTextView;
    private final Handler mHandle = new Handler();

    private ActivityMainBinding binding;

    private String authCode;
    //for scan 2 times
    private boolean isScanningOrderNo;
    private String orderNo;
    private final String scopeBaseUrl = "https://www.googleapis.com/auth/spreadsheets";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = findViewById(R.id.email);
        frameLayout = findViewById(R.id.frameLayout);
        signInButton = findViewById(R.id.sign_in_button);
        sign_out_button = findViewById(R.id.sign_out_button);
        mStatusTextView = findViewById(R.id.status);

        sp = getSharedPreferences(LocalStorage.LOCAL_STORAGE_NAME, Context.MODE_PRIVATE);

        binding.status.setText(sp.getString(LocalStorage.SPREAD_SHEET_ID, ""));

        isScanningOrderNo = true;
        binding.requestText.setText(getString(R.string.action_description_orderNo));

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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(scopeBaseUrl))
                .requestEmail()
                .requestServerAuthCode(Secrets.SERVER_CLIENT_ID)
                .requestIdToken(Secrets.SERVER_CLIENT_ID)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setSize(SignInButton.SIZE_STANDARD);
        // [END customize_button]

        binding.changeId.setOnClickListener(view -> {
            Intent myIntent = new Intent(SignInActivityWithDrive.this, ManageSheetID.class);
            SignInActivityWithDrive.this.startActivity(myIntent);
        });
    }


    private final Runnable mGetToken = this::signIn;

    @Override
    public void onStart() {
        super.onStart();

        // Check if the user is already signed in and all required scopes are granted
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(scopeBaseUrl))) {
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
                    new AlertDialog.Builder(this).setTitle(getString(R.string.CAMERA_PERMISSION_TITLE)).setMessage(getString(R.string.CAMERA_PERMISSION_MESSAGE))
                            .setPositiveButton(getString(R.string.CAMERA_PERMISSION_ALLOW), (dialogInterface, i) -> ActivityCompat.requestPermissions(SignInActivityWithDrive.this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERM))
                            .setNegativeButton(getString(R.string.CAMERA_PERMISSION_DENIED), (dialogInterface, i) -> dialogInterface.dismiss()).create().show();
                } else {
                    new AlertDialog.Builder(this).setTitle(getString(R.string.CAMERA_PERMISSION_TITLE)).setMessage(getString(R.string.CAMERA_PERMISSION_MESSAGE_DENIED))
                            .setPositiveButton(getString(R.string.CAMERA_PERMISSION_ALLOW_SETTINGS), (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();

                            }).setNegativeButton(getString(R.string.CAMERA_PERMISSION_DENIED_EXIT), (dialogInterface, i) -> {
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

    private void signIn() {

        spreadsheetID = binding.status.getText().toString();

        SharedPreferences.Editor editor = sp.edit();
        editor.putString(LocalStorage.SPREAD_SHEET_ID, spreadsheetID);
        editor.apply();

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> updateUI(null));
        mHandle.removeCallbacks(mGetToken);
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                task -> updateUI(null));
    }

    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            mCodeScanner.startPreview();

            authCode = account.getServerAuthCode();
            userName = account.getDisplayName();
            email.setText(account.getEmail());
            email.setVisibility(View.VISIBLE);
            mStatusTextView.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.GONE);
            sign_out_button.setVisibility(View.VISIBLE);
            binding.requestText.setVisibility(View.VISIBLE);
            binding.changeId.setVisibility(View.GONE);

            new Thread(this::getAToken).start();
            mHandle.postDelayed(mGetToken, 3_000_000); // get new access token after 50 minutes
        } else {
            email.setVisibility(View.GONE);
            mStatusTextView.setVisibility(View.VISIBLE);
            signInButton.setVisibility(View.VISIBLE);
            sign_out_button.setVisibility(View.GONE);
            frameLayout.setVisibility(View.GONE);
            binding.requestText.setVisibility(View.GONE);
            binding.changeId.setVisibility(View.VISIBLE);
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
                            Secrets.CLIENT_ID,
                            Secrets.CLIENT_SECRET,
                            authCode,
                            "")
                            .execute();
            accessToken = tokenResponse.getAccessToken();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("Token", accessToken);
    }

    @SuppressLint("SetTextI18n")
    private void onDecoded(Result result) {

        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

        runOnUiThread(()->{
            if (isScanningOrderNo) {
                orderNo = result.getText();

                if (orderNo.startsWith(getString(R.string.check_condition_orderNo_1)) || orderNo.startsWith(getString(R.string.check_condition_orderNo_2))) {
                    isScanningOrderNo = false;
                    binding.requestText.setText(getString(R.string.action_description_machineNo));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mCodeScanner.startPreview();
                } else {
                    new AlertDialog.Builder(this).setTitle(getString(R.string.alert_description_title)).setMessage(getString(R.string.alert_description_orderNo))
                            .setPositiveButton(getString(R.string.alert_description_action), (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                mCodeScanner.startPreview();
                            }).create().show();
                }

            } else {
                String machineNo = result.getText();

                if (machineNo.startsWith(getString(R.string.check_condition_machineNo))) {
                    Intent myIntent = new Intent(SignInActivityWithDrive.this, InputInfomation.class);
                    myIntent.putExtra(LocalStorage.ACCESS_TOKEN, accessToken); //Optional parameters
                    myIntent.putExtra(LocalStorage.SPREAD_SHEET_ID, spreadsheetID); //Optional parameters
                    myIntent.putExtra(LocalStorage.ORDER_MACHINE_NO, orderNo + "|" + machineNo); //Optional parameters
                    myIntent.putExtra(LocalStorage.USER_NAME, userName);
                    isScanningOrderNo = true;
                    binding.requestText.setText(getString(R.string.action_description_orderNo));
                    SignInActivityWithDrive.this.startActivity(myIntent);
                } else {
                    new AlertDialog.Builder(this).setTitle(getString(R.string.alert_description_title)).setMessage(getString(R.string.alert_description_machineNo))
                            .setPositiveButton(getString(R.string.alert_description_action), (dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                mCodeScanner.startPreview();
                            }).create().show();
                }
            }
        });

    }
}
