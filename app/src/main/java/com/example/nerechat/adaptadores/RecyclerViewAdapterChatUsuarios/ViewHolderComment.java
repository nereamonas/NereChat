package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

public class ViewHolderComment  extends RecyclerView.ViewHolder{

    public TextView username, comment, hora;
    public ImageView fotoPerfil;

    public ViewHolderComment(@NonNull View itemView) {
        super(itemView);

        username = itemView.findViewById(R.id.commentUser);
        fotoPerfil = itemView.findViewById(R.id.commentFoto);
        comment = itemView.findViewById(R.id.comment);
        hora = itemView.findViewById(R.id.commentHora);

    }
}
