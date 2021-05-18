package com.example.nerechat.base;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.nerechat.R;

public class BaseFragment extends Fragment {
    //Vamos a crear esta clase que será la base de los fragmentos. Cada vez q se cree un fragment, en vez d heredar directamente de Fragment
    //heredara de BaseFragment, asi podemos editar la creacion conjunta. Queremos kudeatuar el tema y el idioma
    //Lo que conseguimos es, que al apagar la app y volver a arrancarla, se mantengan todas las configuraciones que ha indicado el usuario en las prefereencias. y que estas preferencias se apliquen a todas los fragmentos cuando se crean

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Cogemos de sharedPreferences el tema que tiene el usuario guardado. Y dependiendo del valor, le asignamos al fragmento un tema u otro

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String tema = "";
        if (prefs.contains("tema")) {
            tema = prefs.getString("tema", null);
        }
        switch (tema) {
            case "morado":
                getContext().setTheme(R.style.morado);
                getActivity().setTheme(R.style.morado);
                super.onCreate(savedInstanceState);
                break;
            case "naranja":
                getContext().setTheme(R.style.naranja);
                getActivity().setTheme(R.style.naranja);
                super.onCreate(savedInstanceState);
                break;
            case "verde":
                getContext().setTheme(R.style.verde);
                getActivity().setTheme(R.style.verde);
                super.onCreate(savedInstanceState);
                break;
            case "azul":
                getContext().setTheme(R.style.azul);
                getActivity().setTheme(R.style.azul);
                super.onCreate(savedInstanceState);
                break;
            case "verdeazul":
                getContext().setTheme(R.style.verdeazul);
                getActivity().setTheme(R.style.verdeazul);
                super.onCreate(savedInstanceState);
                break;
            default:
                getContext().setTheme(R.style.azul);
                getActivity().setTheme(R.style.azul);
                super.onCreate(savedInstanceState);
                break;
        }

    }
}