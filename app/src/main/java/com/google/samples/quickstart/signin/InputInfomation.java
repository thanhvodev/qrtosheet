package com.google.samples.quickstart.signin;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.content.Context;
import android.content.SharedPreferences;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InputInfomation extends AppCompatActivity {

    //Binding
    com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding binding;

    //User Info
    private String accessToken;
    private String spreadSheetID;
    private String orderNo;
    private String machineNo;
    private String username;
    private final static String API_KEY = "ACIzaSyE2B_tzd_72dOds0bZwl5o6qwS0NqIOlY";
    
    private SharedPreferences sp;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInputInfomationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Get Information from scan intent
        Bundle extras = getIntent().getExtras();
        accessToken = extras.getString("accessToken");
        spreadSheetID = extras.getString("spreadSheetID");
        
        String[] orderNo_machineNo = extras.getString("orderNo&machineNo").split("\\|");
        orderNo = orderNo_machineNo[0];
        machineNo = orderNo_machineNo[1];
        username = extras.getString("username");

        binding.orderTextView.setText(orderNo);
        binding.machineTextView.setText(machineNo);
        binding.orderRadioButton.setChecked(true);
        binding.standardRadioButton.setChecked(true);
        //For doing request once per day
        sp = getSharedPreferences("localStorage", Context.MODE_PRIVATE);
        boolean isFirstRun = sp.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            new Thread(() -> {
                try {
                    createSheet();
                    addHeaderForSheet();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            setIsFirstRun(false);
        }
        //handle button
        binding.ok.setOnClickListener(view -> handleOK());

        binding.cancelButton.setOnClickListener(view -> {
            finish();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleOK() {
            new Thread(() -> {
                try {
                    int lineNumber =  getLineNumber();
                    appendData(lineNumber);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }).start();

        finish();
    }


    void setIsFirstRun(boolean isFirstRun) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("isFirstRun", isFirstRun);
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private void createSheet() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"requests\": [\r\n    {\r\n      \"addSheet\": {\r\n        \"properties\": {\r\n          \"title\": \"" + "OutputReport" + "\",\r\n          \"gridProperties\": {\r\n            \"rowCount\": 1000,\r\n            \"columnCount\": 26\r\n          },\r\n        }\r\n      }\r\n    }\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetID + ":batchUpdate")
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        Response response = client.newCall(request).execute();

    }

    private void addHeaderForSheet() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \"Thời gian quét\",\r\n      \"Người quét\",\r\n      \"Số Đơn\",\r\n      \"Máy\",\r\n      \"Tên hàng\",\r\n      \"Số tấm\",\r\n      \"Trống\",\r\n      \"Ghi chú\",\r\n      \"Loại hàng\",\r\n      \"Loại công đoạn\"\n    ],\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetID + "/values/" + "OutputReport" + "!A1%3AJ1?includeValuesInResponse=true&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=" + API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        Response response = client.newCall(request).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int getLineNumber() throws IOException, JSONException {

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/"+ spreadSheetID +"/values/OutputReport!B:B?majorDimension=COLUMNS&key=" + API_KEY)
                .method("GET", null)
                .addHeader("Authorization", "Bearer "+accessToken)
                .build();
        Response response = client.newCall(request).execute();

        if (response.code() == 200) {
            JSONObject obj = new JSONObject(Objects.requireNonNull(response.body()).string());
            return obj.getJSONArray("values").get(0).toString().split(",").length+1;
        } else {
            return -1;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void appendData(int lineNumber) throws IOException {
// Read from the database
        String productType = binding.orderRadioButton.isChecked() ? "Đơn" : "Tái chế";
        String stageType = binding.standardRadioButton.isChecked() ? "Chuẩn" : "Thêm";

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \"" + getTime() + "\",\r\n      \"" + username + "\",\r\n      \"" + orderNo + "\",\r\n      \"" + machineNo + "\",\r\n      \"" + binding.tenhangValue.getText().toString() + "\",\r\n      \""+ binding.sotamValue.getText().toString() + "\",\r\n      \"" + binding.trongValue.getText().toString() + "\",\r\n      \"" + binding.ghichuValue.getText().toString() + "\",\r\n      \"" + productType + "\",\r\n      \"" + stageType + "\"\n    ],\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetID + "/values/" + "OutputReport" + "!A" + lineNumber + "%3AJ"+lineNumber +"?includeValuesInResponse=true&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=" + API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        Response response = client.newCall(request).execute();

        if (response.code() == 200) {
            showToastSuccess("Thêm thành công");
        } else {
            setIsFirstRun(true);
            showToastFail("Thêm số đơn '" +orderNo+ "' thất bại!");
        }
    }

    public void showToastSuccess(final String toast) {
        runOnUiThread(() -> {
            Toast.makeText(this, Html.fromHtml("<h2>"+toast+"</h2>"), Toast.LENGTH_SHORT).show();
        });
    }

    public void showToastFail(final String toast) {
        runOnUiThread(() -> { // make toast appear longer
            Toast.makeText(this, Html.fromHtml("<h1>"+toast+"</h1>"), Toast.LENGTH_LONG).show();
            Toast.makeText(this, Html.fromHtml("<h1>"+toast+"</h1>"), Toast.LENGTH_LONG).show();
            Toast.makeText(this, Html.fromHtml("<h1>"+toast+"</h1>"), Toast.LENGTH_SHORT).show();
        });
    }


//    private void addNotification() {
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//        Notification notification = new NotificationCompat.Builder(this, NotificationFailAlert.CHANNEL_ID)
//                .setContentTitle("THÊM THẤT BẠI")
//                .setContentText("Thêm số đơn '" +orderNo+ "', máy '"+ may +"' thất bại! Hãy quét lại!")
//                .setLargeIcon(bitmap)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setSound(uri)
//                .setColor(getResources().getColor(R.color.blue_grey_500))
//                .build();
//
//        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
//        notificationManagerCompat.notify(getNotificationId(), notification);
//
//    }

//    private int getNotificationId() {
//        return (int) new Date().getTime();
//    }
}