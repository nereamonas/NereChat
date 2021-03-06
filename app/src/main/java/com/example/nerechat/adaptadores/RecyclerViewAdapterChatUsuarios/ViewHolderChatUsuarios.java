package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewHolderChatUsuarios extends RecyclerView.ViewHolder {
    //Guardaremos los elementos que tiene cada elemento del recycler view. Es la recycler view tanto del fragment chatAmigos como todos los chats.
    // Se muestra la info de un usuario. un nombre de usuario y la foto de perfil, el ultimo mensaje enviado, la hora del ultimo mensaje,
    // la cantidad de mensajes sin leer y un circulito que muestra si está o no conectado
    public TextView nombreUsuario;
    public TextView info;
    public CircleImageView fotoPerfil;

    public ImageView icRojo;
    public ImageView icVerde;
    public TextView textHoraUltimoMensaje;
    public TextView textViewMensajesSinLeer;

    public ViewHolderChatUsuarios(@NonNull View itemView){
        super(itemView);
        nombreUsuario=itemView.findViewById(R.id.cvNombreUsuario);
        info=itemView.findViewById(R.id.cvInfo);
        fotoPerfil=itemView.findViewById(R.id.cvFoto);
        icRojo=itemView.findViewById(R.id.cvFotoEstadoRojo);
        icVerde=itemView.findViewById(R.id.cvFotoEstadoVerde);
        textHoraUltimoMensaje=itemView.findViewById(R.id.textHoraUltimoMensaje);
        textViewMensajesSinLeer=itemView.findViewById(R.id.textViewMensajesSinLeer);
    }
}