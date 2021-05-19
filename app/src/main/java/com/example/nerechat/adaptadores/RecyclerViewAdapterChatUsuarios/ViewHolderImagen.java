package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.nerechat.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ViewHolderImagen  extends RecyclerView.ViewHolder{

    public ImageView fotoPerfil, fotoPost, mg, mg2, comentar;
    public TextView usuario, usuario2, descripcion, likes;

    public ViewHolderImagen(@NonNull View itemView) {
        super(itemView);

        fotoPerfil = itemView.findViewById(R.id.circleImagePerfil);
        fotoPost = itemView.findViewById(R.id.imageViewPost);
        mg = itemView.findViewById(R.id.imageViewLike);
        mg2 = itemView.findViewById(R.id.imageViewLike2);
        comentar = itemView.findViewById(R.id.imageViewComment);
        usuario = itemView.findViewById(R.id.textViewUsername);
        usuario2 = itemView.findViewById(R.id.textViewUsername2);
        descripcion = itemView.findViewById(R.id.textViewDescripcion);
        likes = itemView.findViewById(R.id.textViewLikes);
    }
}
