package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewHolderChatUsuarios extends RecyclerView.ViewHolder {
    //Guardaremos los elementos que tiene cada elemento del recycler view. en este caso un nombre de usuario, info y la foto de perfil
    public TextView nombreUsuario;
    public TextView info;
    public CircleImageView fotoPerfil;

    public ImageView icRojo;
    public ImageView icVerde;

    public ViewHolderChatUsuarios(@NonNull View itemView){
        super(itemView);
        nombreUsuario=itemView.findViewById(R.id.cvNombreUsuario);
        info=itemView.findViewById(R.id.cvInfo);
        fotoPerfil=itemView.findViewById(R.id.cvFoto);
        icRojo=itemView.findViewById(R.id.cvFotoEstadoRojo);
        icVerde=itemView.findViewById(R.id.cvFotoEstadoVerde);
    }
}