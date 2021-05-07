package com.example.nerechat.ui.chats;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import com.example.nerechat.IniciarSesionActivity;
import com.example.nerechat.PerfilOtroUsActivity;
import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderMensaje;
import com.example.nerechat.helpClass.Mensaje;
import com.example.nerechat.ui.perfil.PerfilFragment;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    //Se muestra el chat con una persona

    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send;
    CircleImageView barraPerfilImg;
    TextView barraUsername,barraStado;

    //Serán los datos del usuario con el que estamos hablando
    String pId,pNombreUsu,pFotoPerfil,pEstado;
    String miFotoPerfil;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef,mDatabaseRefMensajes;
    FirebaseRecyclerOptions<Mensaje> options;
    FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Cogemos el extra que le hemos pasado. Hace referencia al idDelOtroUsuario
        if (getIntent().hasExtra("usuario")) {
            pId = getIntent().getExtras().getString("usuario");
        }


        recyclerView=findViewById(R.id.chat_recyclerView);
        mensaje=findViewById(R.id.chat_mensaje);
        send=findViewById(R.id.chat_send);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        barraPerfilImg=findViewById(R.id.barraImagenPErfil);
        barraUsername=findViewById(R.id.barraNombreUsu);
        barraStado=findViewById(R.id.barraEstado);

        //Cargamos los datos de firebase que necesitaremos
        mAuth=FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMensajes= FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes

        getSupportActionBar().hide();
        //Cargamos el toolbar y cargamos la informacion del otro usaurio en el toolbar
        toolbar=findViewById(R.id.chat_toolbar);
        //setSupportActionBar(findViewById(R.id.chat_toolbar));
        cargarInfoBarra();

        cargarMiFotoPerfil(); //Buscamos mi foto de perfil porque se usara en los mensajes;

        //Cargamos todos los mensajes que se han mandado con ese usuario
        cargarMensajes();

        send.setOnClickListener(new View.OnClickListener() { //Cuando clickemos en el boton send será que queremos mandar un mensaje. Asique mandamos el mensaje
            @Override
            public void onClick(View v) {
                mandarMensaje();
            }
        });

        barraPerfilImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirOtroUsPerfil();
               /* Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chat_to_navigation_perfil, bundle,options);*/
            }
        });
        barraUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirOtroUsPerfil();
/*
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chat_to_navigation_perfil, bundle,options);
*/
            }
        });

    }

    public void abrirOtroUsPerfil(){
        Intent i = new Intent(this, PerfilOtroUsActivity.class);
        //i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("usuario", pId);
        startActivity(i);
        //finish();
    }

    public void cargarInfoBarra(){
        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(pId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Si existe el usuario
                    pNombreUsu=snapshot.child("nombreUsuario").getValue().toString(); //Cogemos su nombre de usuario
                    pFotoPerfil=snapshot.child("fotoPerfil").getValue().toString(); //Cogemos su foto de perfil
                    pEstado=snapshot.child("conectado").getValue().toString(); //Cogemos su estado
                    Picasso.get().load(pFotoPerfil).into(barraPerfilImg); //Mostramos la foto de perfil en pantalla
                    barraUsername.setText(pNombreUsu);//Mostramos el nombre de usuario en pantalla
                    barraStado.setText(pEstado); //Mostramos el estado en pantalla
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    public void cargarMiFotoPerfil(){
        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    //Si existe el usuario
                    miFotoPerfil=snapshot.child("fotoPerfil").getValue().toString(); //Cogemos mi foto de perfil
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void cargarMensajes(){
        //Se ha creado con un recycler view + card view como la lista de usuarios. que firebase facilita el trabajo.
        //Tenemos que crear una clase Chat para guardar los valores de la bbdd
        options= new FirebaseRecyclerOptions.Builder<Mensaje>().setQuery(mDatabaseRefMensajes.child(mUser.getUid()).child(pId), Mensaje.class).build();
        adapter= new FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderMensaje holder, int position, @NonNull Mensaje model) {
                //Tenemos dos opciones. Que el mensaje lo hayamos mandado nosotros, o que nosotros seamos los receptores del mensaje
                if (model.getUsuario().equals(mUser.getUid())){ //En el caso de que el mensaje lo haya mandado yo
                    holder.mensajeTextoUno.setVisibility(View.GONE); //Oculto la info del otro
                    holder.mensajeFotoPerfilUno.setVisibility(View.GONE);
                    holder.mensajeHoraUno.setVisibility(View.GONE);
                    holder.mensajeTextoDos.setVisibility(View.VISIBLE); //Pongo visible la info del mio
                    holder.mensajeFotoPerfilDos.setVisibility(View.VISIBLE);
                    holder.mensajeHoraDos.setVisibility(View.VISIBLE);

                    holder.mensajeTextoDos.setText(model.getMensaje()); //Pongo mi mezu
                    holder.mensajeHoraDos.setText(model.getHora());
                    Picasso.get().load(miFotoPerfil).into(holder.mensajeFotoPerfilDos);
                }else{
                    holder.mensajeTextoUno.setVisibility(View.VISIBLE);//Pongo visible la info del otro usuario
                    holder.mensajeFotoPerfilUno.setVisibility(View.VISIBLE);
                    holder.mensajeHoraUno.setVisibility(View.VISIBLE);
                    holder.mensajeTextoDos.setVisibility(View.GONE);//Oculto la info del mio
                    holder.mensajeFotoPerfilDos.setVisibility(View.GONE);
                    holder.mensajeHoraDos.setVisibility(View.GONE);

                    holder.mensajeTextoUno.setText(model.getMensaje()); //Muestro en la pantalla el mensaje del otro
                    holder.mensajeHoraUno.setText(model.getHora());
                    Picasso.get().load(pFotoPerfil).into(holder.mensajeFotoPerfilUno); //Muestro la foto del otro
                }
            }

            @NonNull
            @Override
            public ViewHolderMensaje onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.mensaje,parent,false);
                return new ViewHolderMensaje(view);
            }
        };

        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }


    public void mandarMensaje(){
        //Mandar un mensaje.
        String mensaj=mensaje.getText().toString();
        if(!mensaj.equals("")){ //El mensaje debe ser distinto de "", sino no se mandara
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");


            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap=new HashMap();
            hashMap.put("mensaje",mensaj);
            hashMap.put("usuario",mUser.getUid());
            hashMap.put("hora",formatter.format(date));
            //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
            mDatabaseRefMensajes.child(pId).child(mUser.getUid()).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //Primero subido corectamente
                    mDatabaseRefMensajes.child(mUser.getUid()).child(pId).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                        @Override
                        public void onSuccess(Object o) {
                            //Segundo subido correctamente
                            mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje
                        }
                    });
                }
            });

        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }


}