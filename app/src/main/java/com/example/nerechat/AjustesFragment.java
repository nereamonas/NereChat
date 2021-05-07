package com.example.nerechat;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class AjustesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}