package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

public class ViewHolderFoto extends RecyclerView.ViewHolder{

    public ImageView img;

    public ViewHolderFoto(@NonNull View itemView) {
        super(itemView);
        img=itemView.findViewById(R.id.imagenGrid);
    }
}
