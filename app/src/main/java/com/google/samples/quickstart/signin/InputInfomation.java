package com.google.samples.quickstart.signin;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.content.Context;
import android.content.SharedPreferences;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final String googleSheetBaseUrl = "https://sheets.googleapis.com/v4/spreadsheets/";
    private final String writeDataOptions = "?includeValuesInResponse=true&responseDateTimeRenderOption=FORMATTED_STRING&responseValueRenderOption=FORMATTED_VALUE&valueInputOption=USER_ENTERED&key=";
    private final int numberOfToast = 3;

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    
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
        spreadSheetID = extras.getString(Constants.SPREAD_SHEET_ID);
        
        String[] orderNo_machineNo = extras.getString("orderNo&machineNo").split("\\|");
        orderNo = orderNo_machineNo[0];
        machineNo = orderNo_machineNo[1];
        username = extras.getString("username");

        binding.orderTextView.setText(orderNo);
        binding.machineTextView.setText(machineNo);
        binding.orderRadioButton.setChecked(true);
        binding.standardRadioButton.setChecked(true);
        //For doing request once per day
        sp = getSharedPreferences(Constants.LOCAL_STORAGE_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = sp.getBoolean(Constants.IS_FIRST_RUN, true);

        if (isFirstRun) {
            new Thread(() -> {
                try {
                    createSheet();
                    addHeaderForSheet();
                } catch (IOException | JSONException e) {
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
        editor.putBoolean(Constants.IS_FIRST_RUN, isFirstRun);
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String getTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    private void createSheet() throws IOException, JSONException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");

        //----Make request-----//
        JSONObject gridProperties = new JSONObject();
        gridProperties.put("rowCount",1000);
        gridProperties.put("columnCount", 26);

        JSONObject properties = new JSONObject();
        properties.put("title", "OutputReport");
        properties.put("gridProperties", gridProperties);

        JSONObject addSheet = new JSONObject();
        addSheet.put("properties", properties);

        JSONObject requestJson = new JSONObject();
        requestJson.put("addSheet", addSheet);

        JSONArray arr = new JSONArray();
        arr.put(requestJson);

        JSONObject requests = new JSONObject();
        requests.put("requests", arr);

        RequestBody body = RequestBody.create(mediaType, requests.toString());
        //----End make request-----//
        Request request = new Request.Builder()
                .url(googleSheetBaseUrl + spreadSheetID + ":batchUpdate")
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
    }

    private void addHeaderForSheet() throws IOException {

        MediaType mediaType = MediaType.parse("application/json");

        Map<String, List<List<String>>> dataMap = new HashMap<>();
        List<String> rowData = Arrays.asList(
                Constants.COL1, Constants.COL2,
                Constants.COL3, Constants.COL4,
                Constants.COL5, Constants.COL6,
                Constants.COL7, Constants.COL8,
                Constants.COL9, Constants.COL10
        );
        dataMap.put("values", Arrays.asList(rowData));

        RequestBody body = RequestBody.create(mediaType, new JSONObject(dataMap).toString());
        Request request = new Request.Builder()
                .url(googleSheetBaseUrl + spreadSheetID + "/values/OutputReport!A1%3AJ1" + writeDataOptions + Secrets.API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();
        client.newCall(request).execute();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int getLineNumber() throws IOException, JSONException {

        Request request = new Request.Builder()
                .url(googleSheetBaseUrl + spreadSheetID +"/values/OutputReport!B:B?majorDimension=COLUMNS&key=" + Secrets.API_KEY)
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
        MediaType mediaType = MediaType.parse("application/json");

        Map<String, List<List<String>>> dataMap = new HashMap<>();
        List<String> rowData = Arrays.asList(
                getTime(), username, orderNo, machineNo,
                binding.productNameEditText.getText().toString(),
                binding.plateNumberEditText.getText().toString(),
                binding.trongValue.getText().toString(),
                binding.noteEditText.getText().toString(),
                productType, stageType
        );
        dataMap.put("values", Arrays.asList(rowData));
        RequestBody body = RequestBody.create(mediaType, new JSONObject(dataMap).toString());
        Request request = new Request.Builder()
                .url(googleSheetBaseUrl + spreadSheetID + "/values/OutputReport!A" + lineNumber + "%3AJ"+ lineNumber + writeDataOptions + Secrets.API_KEY)
                .method("PUT", body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        if (response.code() == 200) {
            showToastSuccess(Constants.SUCCESS);
        } else {
            setIsFirstRun(true);
            showToastFail(Constants.FAIL + orderNo);
        }
    }

    public void showToastSuccess(final String toast) {
        runOnUiThread(() -> {
            Toast.makeText(this, Html.fromHtml("<h2>"+toast+"</h2>"), Toast.LENGTH_SHORT).show();
        });
    }

    public void showToastFail(final String toast) {
        runOnUiThread(() -> { // make toast appear longer
            for (int i = 0; i < numberOfToast; i++) {
                Toast.makeText(this, Html.fromHtml("<h1>"+toast+"</h1>"), Toast.LENGTH_LONG).show();
            }
        });
    }
}