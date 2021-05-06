package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewHolderChat extends RecyclerView.ViewHolder {
    //Guardaremos los elementos que tiene cada elemento del recycler view.

    public CircleImageView mensajeFotoPerfilUno;
    public CircleImageView mensajeFotoPerfilDos;
    public TextView mensajeTextoUno;
    public TextView mensajeTextoDos;

    public ViewHolderChat(@NonNull View itemView){
        super(itemView);
        mensajeFotoPerfilUno=itemView.findViewById(R.id.mensajeFotoPerfilUno);
        mensajeFotoPerfilDos=itemView.findViewById(R.id.mensajeFotoPerfilDos);
        mensajeTextoUno=itemView.findViewById(R.id.mensajeTextoUno);
        mensajeTextoDos=itemView.findViewById(R.id.mensajeTextoDos);
    }
}