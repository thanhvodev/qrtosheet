package com.google.samples.quickstart.signin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.google.samples.quickstart.signin.databinding.ActivityMainBinding;
import com.google.samples.quickstart.signin.databinding.ActivityManageSheetIdBinding;

public class ManageSheetID extends AppCompatActivity {
    private ActivityManageSheetIdBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageSheetIdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.kiemTra.setOnClickListener(view -> {
            if (binding.passwordTextView.getText().toString().equals(Secrets.PASSWORD)) {
                Intent myIntent = new Intent(ManageSheetID.this, ChangeSheetId.class);
                ManageSheetID.this.startActivity(myIntent);
                finish();
            } else {
                Toast.makeText(this, Constants.WRONG_PASSWORD_TEXT, Toast.LENGTH_SHORT).show();
            }
        });

    }
}