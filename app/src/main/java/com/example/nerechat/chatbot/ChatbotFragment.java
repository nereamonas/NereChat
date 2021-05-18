package com.example.nerechat.chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderMensaje;
import com.example.nerechat.helpClass.Mensaje;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;

import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatbotFragment extends Fragment implements BotReply {


    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send;
    CircleImageView barraPerfilImg;
    TextView barraUsername,barraStado;

    String miFotoPerfil;

    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private String uuid = UUID.randomUUID().toString();
    private String TAG = "mainactivity";

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef,mDatabaseRefMensajes;

    FirebaseRecyclerOptions<Mensaje> options;
    FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje> adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root=inflater.inflate(R.layout.fragment_chatbot, container, false);


        recyclerView=root.findViewById(R.id.chat_recyclerViewchatbot);
        mensaje=root.findViewById(R.id.chat_mensajechatbot);
        send=root.findViewById(R.id.chat_sendchatbot);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMensajes= FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes

        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

        toolbar=root.findViewById(R.id.chat_toolbarchatbot);
        barraPerfilImg=root.findViewById(R.id.barraImagenPErfil);
        barraUsername=root.findViewById(R.id.barraNombreUsu);
        barraStado=root.findViewById(R.id.barraEstado);
        barraPerfilImg.setImageResource(R.mipmap.ic_fotochatbot_round);
        barraUsername.setText("Chatbot");
        barraStado.setText("Conectado");
        cargarMiFotoPerfil();
        cargarMensajes();

        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mandarMensaje();
            }
        });

        setUpBot();

        return root;
    }

    public void mandarMensaje(){
        String mensaj=mensaje.getText().toString();
        if(!mensaj.equals("")){ //El mensaje debe ser distinto de "", sino no se mandara
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");

            sendMessageToBot(mensaj);
            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap=new HashMap();
            hashMap.put("mensaje",mensaj);
            hashMap.put("usuario",mUser.getUid());
            hashMap.put("hora",formatter.format(date));
            //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
            mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot").push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //subido corectamente
                    mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje

                }
            });

        }

    }


    private void setUpBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList(getString(R.string.googleApis)));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);

            Log.d(TAG, "projectId : " + projectId);
        } catch (Exception e) {
            Log.d(TAG, "setUpBot: " + e.getMessage());
        }
    }


    public void cargarMensajes(){
        //Se ha creado con un recycler view + card view como la lista de usuarios. que firebase facilita el trabajo.
        //Tenemos que crear una clase Chat para guardar los valores de la bbdd
        options= new FirebaseRecyclerOptions.Builder<Mensaje>().setQuery(mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot"), Mensaje.class).build();
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

                    holder.imageLikeDos.setVisibility(View.GONE);
                    holder.imageLikeUno.setVisibility(View.GONE);
                    holder.imageDobleCheckDos.setVisibility(View.GONE);

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

                    holder.imageLikeDos.setVisibility(View.GONE);
                    holder.imageLikeUno.setVisibility(View.GONE);
                    holder.imageDobleCheckDos.setVisibility(View.GONE);

                    holder.mensajeTextoUno.setText(model.getMensaje()); //Muestro en la pantalla el mensaje del otro
                    holder.mensajeHoraUno.setText(model.getHora());
                    holder.mensajeFotoPerfilUno.setImageResource(R.mipmap.ic_fotochatbot_round);
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

    public void cargarMiFotoPerfil(){
        //Tenemos que coger de la base de datos la informacion del otro usuario. como tenemos su UID es sencillo
        mDatabaseRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
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
    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        new SendMessageInBackground(this, sessionName, sessionsClient, input).execute();
    }

    @Override
    public void callback(DetectIntentResponse returnResponse) {
        if(returnResponse!=null) {
            String botReply = returnResponse.getQueryResult().getFulfillmentText();
            if(!botReply.isEmpty()){

                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
                HashMap hashMap=new HashMap();
                hashMap.put("mensaje",botReply);
                hashMap.put("usuario","chatbot");
                hashMap.put("hora",formatter.format(date));
                //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
                mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot").push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        //subido corectamente
                        mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje

                    }
                });

            }else {
                Toast.makeText(getContext(), "Algo salio mal", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No se ha podido conectar", Toast.LENGTH_SHORT).show();
        }
    }

}