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

    //Será el fragment encargado de manejar la conversación entre el usuario y el chatbot. Contendrá un toolbar que se personalizará con la foto y el estado del chatbot
    // una recyclerview donde se cargan todos los mensajes intercambiados y en la parte inferior un edittext para escribir el mensaje
    //que se quiere enviar al boton y un boton para enviar el mensaje

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

        //Hasieratuamos todos los elementos necesarios
        recyclerView=root.findViewById(R.id.chat_recyclerViewchatbot);
        mensaje=root.findViewById(R.id.chat_mensajechatbot);
        send=root.findViewById(R.id.chat_sendchatbot);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //Cogemos el usuario actual que tiene iniciada la sesion
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMensajes= FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes

        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE); //Ocultaremos la barra de navegacion inferior, ya que estando en el chat no nos interesa mostrarla y ganamos espacio

        toolbar=root.findViewById(R.id.chat_toolbarchatbot);
        barraPerfilImg=root.findViewById(R.id.barraImagenPErfil);
        barraUsername=root.findViewById(R.id.barraNombreUsu);
        barraStado=root.findViewById(R.id.barraEstado);
        //Añadimos la informacion de la barra. la foto del chatbot, el nombre y el estado(siempre estará conectado)
        barraPerfilImg.setImageResource(R.mipmap.ic_fotochatbot_round);
        barraUsername.setText("Chatbot");
        barraStado.setText("Conectado");

        cargarMiFotoPerfil();//Cargamos desde firebase nuestra foto de perfil, ya que cada vez que mandamos un mensaje mostraremos nuestra foto, asi solo realizaremos la llamada una vez y optimizaremos tiempo
        cargarMensajes();//Cargamos todos los mensajes almacenados en firebase que comparten el usuario y el chatbot

        send.setOnClickListener(new View.OnClickListener() { //Cuando clickemos el boton send mandaremos el mensaje
            @Override public void onClick(View view) {
                mandarMensaje();
            }
        });

        setUpBot();

        return root;
    }

    public void mandarMensaje(){
        String mensaj=mensaje.getText().toString(); //Cogemos el texto escrito en el edittext
        if(!mensaj.equals("")){ //El mensaje debe ser distinto de "", sino no se mandara
            Date date = new Date(); //Cogeremos la hora exacta del momento en el que se mande, para saber a que hora se ha mandado el mensaje exactamente
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");

            sendMessageToBot(mensaj); //Le mandamos el mensaje al bot. el nos dará la respuesta
            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado, la hora y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap=new HashMap();
            hashMap.put("mensaje",mensaj);
            hashMap.put("usuario",mUser.getUid());
            hashMap.put("hora",formatter.format(date));
            //subimos con el titulo de mi usuario y subtitulo de chatbot.
            mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot").push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) { //Una vez subido
                    //subido corectamente
                    mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje
                }
            });

        }

    }


    private void setUpBot() { //Creamos la conexion con el chatbot. está creado con dialogflow que ofrece google
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


    public void cargarMensajes(){ //Cargamos todos los mensajes almacenados en firebase
        //Se ha creado con un recycler view + card view . que firebase facilita el trabajo.
        //Tenemos que crear una clase Mensaje para guardar los valores de la bbdd
        options= new FirebaseRecyclerOptions.Builder<Mensaje>().setQuery(mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot"), Mensaje.class).build();
        adapter= new FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderMensaje holder, int position, @NonNull Mensaje model) {
                //Tenemos dos opciones. Que el mensaje lo hayamos mandado nosotros, o que nosotros seamos los receptores del mensaje
                if (model.getUsuario().equals(mUser.getUid())){ //En el caso de que el mensaje lo haya mandado yo
                    holder.mensajeTextoUno.setVisibility(View.GONE); //Oculto la info de cuando el mensaje lo envia el otro usuario
                    holder.mensajeFotoPerfilUno.setVisibility(View.GONE);
                    holder.mensajeHoraUno.setVisibility(View.GONE);
                    holder.mensajeTextoDos.setVisibility(View.VISIBLE); //Pongo visible los elementos del mensaje cuando lo envio yo
                    holder.mensajeFotoPerfilDos.setVisibility(View.VISIBLE);
                    holder.mensajeHoraDos.setVisibility(View.VISIBLE);

                    holder.imageLikeDos.setVisibility(View.GONE);
                    holder.imageLikeUno.setVisibility(View.GONE);
                    holder.imageDobleCheckDos.setVisibility(View.GONE);

                    holder.mensajeTextoDos.setText(model.getMensaje()); //Escribo mi mensaje en mensajeTextoDos ya que todos los elementos Dos perteneceran a mi mensaje
                    holder.mensajeHoraDos.setText(model.getHora()); //Pongo la hora
                    Picasso.get().load(miFotoPerfil).into(holder.mensajeFotoPerfilDos); //Añado la imagen
                }else{ //El mensaje no lo he mandado yo por lo que se hará lo contrario, los elementos que terminen en Uno visibles y los elementos que terminan en Dos se omiten
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
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onItemRangeInserted(int positionStart, int itemCount) { //Este metodo es para que cada vez que se mande un mensaje se haga scroll hasta el final del chat, por defecto se quedaria en la pos 1.
                ((LinearLayoutManager)recyclerView.getLayoutManager()).scrollToPositionWithOffset(adapter.getItemCount() - 1,200);
            }
        });
        adapter.startListening(); //Empieza a escuchar
        recyclerView.setAdapter(adapter); //Añadimos el adaptador a la recyclerview
    }

    public void cargarMiFotoPerfil(){
        //Tenemos que coger de la base de datos mi foto de perfil. como tenemos su UID es sencillo
        mDatabaseRef.child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() { //Añadimos un listener de solo una vez.
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){ //Si existe un elemento con las caracteristicas que hemos dicho
                    //Si existe el usuario
                    miFotoPerfil=snapshot.child("fotoPerfil").getValue().toString(); //Cogemos mi foto de perfil y la guardamos en un atr
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void sendMessageToBot(String message) { //Le mandamos el mensaje al bot
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        new SendMessageInBackground(this, sessionName, sessionsClient, input).execute();
    }

    @Override
    public void callback(DetectIntentResponse returnResponse) {
        //Recogemos la respuesta del bot
        if(returnResponse!=null) {
            String botReply = returnResponse.getQueryResult().getFulfillmentText();
            if(!botReply.isEmpty()){

                Date date = new Date(); //Cogemos la hora exacta en la que estamos
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
                HashMap hashMap=new HashMap();
                hashMap.put("mensaje",botReply);
                hashMap.put("usuario","chatbot"); //En este caso como lo envia el chatbot, ponemos chatbot asecas
                hashMap.put("hora",formatter.format(date));
                //añadimos el mensaje a la base de datos
                mDatabaseRefMensajes.child(mUser.getUid()).child("chatbot").push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {//Cuando se haya añadido
                        //subido corectamente
                        mensaje.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje
                    }
                });

            }else {
                Toast.makeText(getContext(), getString(R.string.toast_algosaliomal), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.toast_Nosehapodidoconectarconelchatbot), Toast.LENGTH_SHORT).show();
        }
    }

}