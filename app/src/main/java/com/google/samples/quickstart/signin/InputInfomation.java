package com.google.samples.quickstart.signin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding;

public class InputInfomation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.google.samples.quickstart.signin.databinding.ActivityInputInfomationBinding binding = ActivityInputInfomationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //handle button
        binding.ok.setOnClickListener(view -> {
            handleOK();
        });

        binding.huy.setOnClickListener(view -> {
            handleHuy();
        });
    }

    private void handleOK() {
        finish();
    }

    private void handleHuy() {
        finish();
    }
}