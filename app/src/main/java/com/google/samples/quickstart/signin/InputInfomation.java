package com.google.samples.quickstart.signin;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InputInfomation extends AppCompatActivity{

    //Binding
    com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding binding;

    //User Info
    private String accessToken = "";
    private String spreadSheetID = "";
    private String soDon = "";
    private String username = "";
    private final static String API_KEY = "AIzaSyCE2B_tzd_72dOds0bZwl5o6qwS0NqIOlY";

    //For doing request once per day
    private int currentDay;
    private int lastDay;
    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInputInfomationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Get Information from scan intent
        Bundle extras = getIntent().getExtras();
        accessToken = extras.getString("accessToken");
        spreadSheetID = extras.getString("spreadSheetID");
        soDon = extras.getString("soDon");
        username = extras.getString("username");

        //Set Field for số Đơn và Máy và Đơn hàng
        binding.sodonValue.setText(soDon);
        binding.mayValue.setText(Build.DISPLAY);
        binding.don.setChecked(true);
        //For doing request once per day
        sp = getSharedPreferences("localStorage", Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        lastDay = sp.getInt("day", Context.MODE_PRIVATE);

        //handle button
        binding.ok.setOnClickListener(view -> {
            handleOK();
        });

        binding.huy.setOnClickListener(view -> {
            finish();
        });
    }

    private void handleOK(){
        // create a page in sheet once a day
        if (currentDay != lastDay) {
            setLastDay();
            new Thread(()->{
                try {
                    createSheetPerDay();
                    addHeaderForSheet();
                    appendData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        new Thread(()->{
            try {
                appendData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Toast.makeText(InputInfomation.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
        finish();
    }

    //Update lastDay to currentDay
    void setLastDay() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("day", currentDay);
        editor.apply();
    }


    //API CALL

    @SuppressLint("NewApi")
    private static String getDate() {
        return String.valueOf(LocalDate.now());
    }
    private static String getTime() { return String.valueOf(Calendar.getInstance().getTime()); }
    private void createSheetPerDay() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"requests\": [\r\n    {\r\n      \"addSheet\": {\r\n        \"properties\": {\r\n          \"title\": \"" + getDate() + "\",\r\n          \"gridProperties\": {\r\n            \"rowCount\": 20,\r\n            \"columnCount\": 12\r\n          },\r\n        }\r\n      }\r\n    }\r\n  ]\r\n}");
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
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \"Thời gian quét\",\r\n      \"Người quét\",\r\n      \"Máy quét\",\r\n      \"Số Đơn\",\r\n      \"Tên hàng\",\r\n      \"Số tấm\",\r\n      \"Trống\",\r\n      \"Ghi chú\",\r\n      \"Loại hàng\"\r\n    ],\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetID + "/values/" + getDate() + "!A1%3AI1?includeValuesInResponse=true&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=" + API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        Response response = client.newCall(request).execute();
    }

    private void appendData() throws IOException {

        String loaiHang = binding.don.isChecked()? "Đơn" : "Tái chế";

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"values\": [\r\n    [\r\n      \""+ getTime() +"\",\r\n      \""+ username +"\",\r\n      \" "+ Build.DISPLAY + "\",\r\n      \"" + soDon + "\",\r\n      \"" + binding.tenhangValue.getText().toString() + "\",\r\n      \""+ binding.sotamValue.getText().toString() +"\",\r\n      \""+ binding.trongValue.getText().toString() +"\",\r\n      \""+ binding.ghichuValue.getText().toString() +"\",\r\n      \""+ loaiHang +"\"\r\n    ]\r\n  ]\r\n}");
        Request request = new Request.Builder()
                .url("https://sheets.googleapis.com/v4/spreadsheets/"+ spreadSheetID +"/values/" + getDate() + ":append?includeValuesInResponse=true&insertDataOption=INSERT_ROWS&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key="+API_KEY)
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "text/plain")
                .build();
        Response response = client.newCall(request).execute();

    }
}