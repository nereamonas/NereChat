package com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewHolderMensaje extends RecyclerView.ViewHolder {
    //Guardaremos los elementos que tiene cada elemento del recycler view. En este caso hace referencia a un mensaje. El mensaje puede ser enviado por ti
    // o por la otra persona, por esa razon tendremos los elementos repetidos con uno o dos para depende de la persona que lo mande mostrar unos u otros
    // se pondra una foto de perfil, el texto del mensaje, la hora en la que se envia, la reaccion al mensaje en una imagen y el doblecheck azul en otra imagen

    public CircleImageView mensajeFotoPerfilUno;
    public CircleImageView mensajeFotoPerfilDos;
    public TextView mensajeTextoUno;
    public TextView mensajeTextoDos;
    public TextView mensajeHoraUno;
    public TextView mensajeHoraDos;
    public ImageView imageLikeUno;
    public ImageView imageLikeDos;
    public ImageView imageDobleCheckDos;


    public ViewHolderMensaje(@NonNull View itemView){
        super(itemView);
        mensajeFotoPerfilUno=itemView.findViewById(R.id.mensajeFotoPerfilUno);
        mensajeFotoPerfilDos=itemView.findViewById(R.id.mensajeFotoPerfilDos);
        mensajeTextoUno=itemView.findViewById(R.id.mensajeTextoUno);
        mensajeTextoDos=itemView.findViewById(R.id.mensajeTextoDos);
        mensajeHoraUno=itemView.findViewById(R.id.mensajeHoraUno);
        mensajeHoraDos=itemView.findViewById(R.id.mensajeHoraDos);
        imageLikeUno=itemView.findViewById(R.id.imageLikeUno);
        imageLikeDos=itemView.findViewById(R.id.imageLikeDos);
        imageDobleCheckDos=itemView.findViewById(R.id.imageDobleCheckDos);
    }
}