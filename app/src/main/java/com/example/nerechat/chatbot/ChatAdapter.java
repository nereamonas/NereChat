package com.example.nerechat.chatbot;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderMensaje;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {

    private List<MessageBot> messageList;
    private Activity activity;

    String fotoPerfil="";

    public ChatAdapter(List<MessageBot> messageList, Activity activity) {
        this.messageList = messageList;
        this.activity = activity;

        FirebaseAuth mAuth= FirebaseAuth.getInstance();
        FirebaseUser mUser=mAuth.getCurrentUser();
        DatabaseReference mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRef.child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    fotoPerfil=snapshot.child("fotoPerfil").getValue().toString(); //Cogemos su foto de perfil
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.mensaje, parent, false);
        return new MyViewHolder(view);

    }

    @Override public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String message = messageList.get(position).getMessage();
        boolean isReceived = messageList.get(position).getIsReceived();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        String hora=formatter.format(date);
        if(!isReceived){
            holder.mensajeTextoUno.setVisibility(View.GONE); //Oculto la info del otro
            holder.mensajeFotoPerfilUno.setVisibility(View.GONE);
            holder.mensajeHoraUno.setVisibility(View.GONE);
            holder.mensajeTextoDos.setVisibility(View.VISIBLE); //Pongo visible la info del mio
            holder.mensajeFotoPerfilDos.setVisibility(View.VISIBLE);
            holder.mensajeHoraDos.setVisibility(View.VISIBLE);

            holder.mensajeTextoDos.setText(message); //Pongo mi mezu
            holder.mensajeHoraDos.setText(hora);
            Picasso.get().load(fotoPerfil).into(holder.mensajeFotoPerfilDos);

        }else {
            holder.mensajeTextoUno.setVisibility(View.VISIBLE);//Pongo visible la info del otro usuario
            holder.mensajeFotoPerfilUno.setVisibility(View.VISIBLE);
            holder.mensajeHoraUno.setVisibility(View.VISIBLE);
            holder.mensajeTextoDos.setVisibility(View.GONE);//Oculto la info del mio
            holder.mensajeFotoPerfilDos.setVisibility(View.GONE);
            holder.mensajeHoraDos.setVisibility(View.GONE);

            holder.mensajeTextoUno.setText(message); //Muestro en la pantalla el mensaje del otro
            holder.mensajeHoraUno.setText(hora);
            holder.mensajeFotoPerfilUno.setImageResource(R.mipmap.ic_fotochatbot_round);
            //Picasso.get().load(getResources().getDrawable(R.drawable.ic_fotochatbot_background)).into(holder.mensajeFotoPerfilUno); //Muestro la foto del otro

        }
    }

    @Override public int getItemCount() {
        return messageList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder{

        public CircleImageView mensajeFotoPerfilUno;
        public CircleImageView mensajeFotoPerfilDos;
        public TextView mensajeTextoUno;
        public TextView mensajeTextoDos;
        public TextView mensajeHoraUno;
        public TextView mensajeHoraDos;

        public MyViewHolder(@NonNull View itemView){
            super(itemView);
            mensajeFotoPerfilUno=itemView.findViewById(R.id.mensajeFotoPerfilUno);
            mensajeFotoPerfilDos=itemView.findViewById(R.id.mensajeFotoPerfilDos);
            mensajeTextoUno=itemView.findViewById(R.id.mensajeTextoUno);
            mensajeTextoDos=itemView.findViewById(R.id.mensajeTextoDos);
            mensajeHoraUno=itemView.findViewById(R.id.mensajeHoraUno);
            mensajeHoraDos=itemView.findViewById(R.id.mensajeHoraDos);
        }
    }

}
