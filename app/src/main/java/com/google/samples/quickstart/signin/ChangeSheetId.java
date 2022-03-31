package com.google.samples.quickstart.signin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("/");

        binding.doneInputId.setOnClickListener(view -> {
            String spreadSheetId = binding.spreadsheetId.getText().toString();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("SPREADSHEETID", spreadSheetId);
            editor.putBoolean("isFirstRun", true);
            editor.apply();
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        myRef.child(spreadSheetId).setValue(2);
                    } else {
                        // Don't exist! Do something.
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed, how to handle?

                }

            });
            Intent myIntent = new Intent(ChangeSheetId.this, SignInActivityWithDrive.class);
            ChangeSheetId.this.startActivity(myIntent);
            finish();
        });
    }
}