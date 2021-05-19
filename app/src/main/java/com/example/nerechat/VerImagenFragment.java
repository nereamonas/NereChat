package com.example.nerechat;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nerechat.helpClass.ZoomImageView;
import com.squareup.picasso.Picasso;

public class VerImagenFragment extends Fragment {

    //Es un fragment donde simplemente se visualiza una imagen en grande. Será un zoomimageview lo que nos permitirá
    // hacer zoom sobre la propia imagen.

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root=inflater.inflate(R.layout.fragment_ver_imagen, container, false);
        String imagen = getArguments().getString("imagen"); //Cogemos el enlace de la foto que hay que mostrar que se pasará por parametro
        ZoomImageView zoomImageView= root.findViewById(R.id.zoomImageView);
        Picasso.get().load(imagen).into(zoomImageView); //Mostramos la url de la foto en el zoomimageview



        return root;
    }
}