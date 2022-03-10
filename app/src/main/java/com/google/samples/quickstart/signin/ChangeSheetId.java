package com.google.samples.quickstart.signin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.samples.quickstart.signin.databinding.ActivityChangeSheetIdBinding;

public class ChangeSheetId extends AppCompatActivity {
    private ActivityChangeSheetIdBinding binding;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangeSheetIdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sp = getSharedPreferences("localStorage", Context.MODE_PRIVATE);

        binding.doneInputId.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("SPREADSHEETID", binding.spreadsheetId.getText().toString());
            editor.apply();
            Intent myIntent = new Intent(ChangeSheetId.this, SignInActivityWithDrive.class);
            ChangeSheetId.this.startActivity(myIntent);
            finish();
        });
    }
}