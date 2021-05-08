package com.example.nerechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;

public class AjustesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }


    @Override
    public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String s){
        switch (s) {
            case "idioma":
                //A cambiado el valor del idioma. Pues aplicamos la configuracion para cambiar el idioma
                //Como hemos creado esa clase de herencia, solo tenemos q reiniciar el intent y ya se aplican los cambios. puesto q cada vez q se crea, mirara en los ajustes el idioma establecido
                Log.d("Logs", "cambio idioma");
                reload();
                break;
            case "tema":
                //A cambiado el tema
                //Lo mismo q el idioma, realmente, solo tenemos q reiniciar el intent porq ya aplicamos la configuracion al crearlo. sino esa configuracions  aplica aqui
                Log.d("Logs", "cambio tema");
                reload();
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume () {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onPause () {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void reload(){
        //Reiniciamos el intent. y le pasamos el parametro ajustes, para q lo inicialice en este fragment
        getActivity().finish();
        startActivity(getActivity().getIntent());
        Intent i = new Intent(getActivity(), MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("abrir", "ajustes");
        startActivity(i);
    }

}