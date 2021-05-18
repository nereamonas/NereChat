package com.example.nerechat.base;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.nerechat.R;

import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    //Vamos a crear esta clase que será la base de las actividades. Cada vez q se cree una activity, en vez d heredar directamente de AppCompatActivity
    //heredara de BaseActivity, asi podemos editar la creacion conjunta. Queremos kudeatuar el tema y el idioma
    //Lo que conseguimos es, que al apagar la app y volver a arrancarla, se mantengan todas las configuraciones que ha indicado el usuario en las prefereencias. y que estas preferencias se apliquen a todas las actividades cuando se crean

        @Override
        public void onCreate(Bundle savedInstanceState) {
            //Cogemos de sharedPreferences el tema que tiene el usuario guardado. Y dependiendo del valor, le asignamos a la actividad un tema u otro

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String tema = "";
            if (prefs.contains("tema")) {
                tema = prefs.getString("tema", null);
            }
            switch (tema) {
                case "morado":
                    this.setTheme(R.style.morado);
                    super.onCreate(savedInstanceState);
                    break;
                case "naranja":
                    this.setTheme(R.style.naranja);
                    super.onCreate(savedInstanceState);
                    break;
                case "verde":
                    this.setTheme(R.style.verde);
                    super.onCreate(savedInstanceState);
                    break;
                case "azul":
                    this.setTheme(R.style.azul);
                    super.onCreate(savedInstanceState);
                    break;
                case "verdeazul":
                    this.setTheme(R.style.verdeazul);
                    super.onCreate(savedInstanceState);
                    break;
                default:
                    this.setTheme(R.style.azul);
                    super.onCreate(savedInstanceState);
                    break;
            }


            //Haremos lo mismo con el idioma
            String idioma = "";
            if (prefs.contains("idioma")) {
                idioma = prefs.getString("idioma", null);
            }

            Locale locale = new Locale(idioma);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(config, null);

        }

    }