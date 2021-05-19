package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.nerechat.R;
import com.example.nerechat.helpClass.ZoomImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ViewHolderImagen  extends RecyclerView.ViewHolder{

    public ZoomImageView fotoPost;
    public ImageView fotoPerfil, mg, mg2, comentar, borrar;
    public TextView usuario, usuario2, descripcion, likes;

    public ViewHolderImagen(@NonNull View itemView) {
        super(itemView);

        fotoPerfil = itemView.findViewById(R.id.circleImagePerfil);
        fotoPost = itemView.findViewById(R.id.imageViewPost);
        mg = itemView.findViewById(R.id.imageViewLike);
        mg2 = itemView.findViewById(R.id.imageViewLike2);
        comentar = itemView.findViewById(R.id.imageViewComment);
        borrar = itemView.findViewById(R.id.imageViewBasura);
        usuario = itemView.findViewById(R.id.textViewUsername);
        usuario2 = itemView.findViewById(R.id.textViewUsername2);
        descripcion = itemView.findViewById(R.id.textViewDescripcion);
        likes = itemView.findViewById(R.id.textViewLikes);
    }
}
