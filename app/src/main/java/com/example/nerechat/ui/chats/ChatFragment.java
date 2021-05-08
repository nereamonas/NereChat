package com.example.nerechat.ui.chats;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderMensaje;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Mensaje;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFragment extends Fragment {

    private BaseViewModel chatViewModel;

    //Se muestra el chat con una persona

    Toolbar toolbar;
    RecyclerView recyclerView;
    EditText mensaje;
    ImageView send;
    CircleImageView barraPerfilImg;
    TextView barraUsername,barraStado;

    //Serán los datos del usuario con el que estamos hablando
    String pId,pNombreUsu,pFotoPerfil,pEstado;
    String miFotoPerfil, miNombreUsuario;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef,mDatabaseRefMensajes;
    FirebaseRecyclerOptions<Mensaje> options;
    FirebaseRecyclerAdapter<Mensaje, ViewHolderMensaje> adapter;

    String UrlNotif="https://fcm.googleapis.com/fcm/send";
    RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        chatViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root=inflater.inflate(R.layout.fragment_chat, container, false);



        //Cogemos el extra que le hemos pasado. Hace referencia al idDelOtroUsuario

        pId = getArguments().getString("usuario");

        recyclerView=root.findViewById(R.id.chat_recyclerView2);
        mensaje=root.findViewById(R.id.chat_mensaje2);
        send=root.findViewById(R.id.chat_send2);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        barraPerfilImg=root.findViewById(R.id.barraImagenPErfil);
        barraUsername=root.findViewById(R.id.barraNombreUsu);
        barraStado=root.findViewById(R.id.barraEstado);

        //Cargamos los datos de firebase que necesitaremos
        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //La base de datos perfil
        mDatabaseRefMensajes= FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes
        cambiarEstado("Conectado");

        requestQueue= Volley.newRequestQueue(getContext());
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        //Cargamos el toolbar y cargamos la informacion del otro usaurio en el toolbar
        toolbar=root.findViewById(R.id.chat_toolbar2);
        //setSupportActionBar(findViewById(R.id.chat_toolbar));

        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

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
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_chatFragment_to_perfilOtroUsFragment, bundle,options);
            }
        });
        barraUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                bundle.putString("usuario", pId);
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_chatFragment_to_perfilOtroUsFragment, bundle,options);
            }
        });
        return root;

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

                    //Mirar si tiene la ultima hora para mostrarla
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
                    if (prefs.contains("ultimaHora")) { //Comprobamos si existe notif
                        Boolean bloqueado = prefs.getBoolean("ultimaHora", true);  //Comprobamos si las notificaciones estan activadas
                        Log.d("Logs", "estado visulizar ultimaHora: " + bloqueado);
                        if (bloqueado) { //Si esta bloqueado no mostramos nada
                            barraStado.setText("");
                        }else{
                            barraStado.setText(pEstado); //no estan bloqueados asiq mostramos
                        }
                    }
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
                    miNombreUsuario=snapshot.child("nombreUsuario").getValue().toString();
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

                            mandarNotificacion(mensaj);
                        }
                    });
                }
            });

        }
    }

    public void mandarNotificacion(String mensaje){
        JSONObject jsonObject= new JSONObject();
        try{
            jsonObject.put("to","/topics/"+pId);
            JSONObject jsonObject1= new JSONObject();
            jsonObject1.put("title","Mensaje de: "+miNombreUsuario);
            jsonObject1.put("body",mensaje);

            JSONObject jsonObject2= new JSONObject();
            jsonObject2.put("userID",mUser.getUid());
            jsonObject2.put("type","sms");


            jsonObject.put("notification",jsonObject1);
            jsonObject.put("data",jsonObject2);

            JsonObjectRequest request= new JsonObjectRequest(Request.Method.POST, UrlNotif, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                public Map<String,String> getHeaders() throws AuthFailureError {
                    Map<String,String> map= new HashMap<>();
                    map.put("content-type","application/json");
                    map.put("authorization","key=AAAAiLCQj0o:APA91bEjfbUQE8VxAzxMDJZ8RpftfBB0qalOOiL-y-BMhY45Tk6pMakAtwxaDoI2SSeIjN4AYKPQLYt_yuL7Wy4N2KnbRb6IJ7IQB-iyOxPjGpnzcKCYdgWf0CyZcvMO-O7guX_KOjq6");
                    return map;
                }
            };
            requestQueue.add(request);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }



    public void cambiarEstado(String estado){
        mDatabaseRef.child(mUser.getUid()).child("conectado").setValue(estado);
    }


}