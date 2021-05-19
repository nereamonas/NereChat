package com.example.nerechat.ui.chats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderChatUsuarios;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Mensaje;
import com.example.nerechat.helpClass.Usuario;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatsAmigosFragment extends Fragment {

    private BaseViewModel chatsAmigosViewModel;
    //Se muestra un recyclerview + card view de todos los perfiles con los que podemos hablar

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef,mDatabaseRefMensajes;
    FirebaseRecyclerOptions<Usuario> options;
    FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios> adapter;

    RecyclerView recyclerView;
    DividerItemDecoration dividerItemDecoration;
    FloatingActionButton floatButton,imgChatbot;
    TextView nohaychats;

    EditText toolbarSearchEditText;
    ImageView toolbarImageSearch, toolbarImagenAjustes;
    TextView toolbarTitulo;
    Toolbar toolbar;
    ConstraintLayout constraintLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        chatsAmigosViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_chatsamigos, container, false);


        //Inicializamos las variables
        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos donde se encuentras los perfiles de usuario
        mDatabaseRefMensajes= FirebaseDatabase.getInstance().getReference().child("MensajesChat"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes

        recyclerView=root.findViewById(R.id.recycleViewContactos);
        LinearLayoutManager llm=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        dividerItemDecoration= new DividerItemDecoration(getContext(),llm.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        nohaychats=root.findViewById(R.id.textViewNoTienesChats);
        nohaychats.setVisibility(View.VISIBLE); //Cuando no hay todavia ningun chat, mostraremos este textview para q el usuario vea que esta en blanco porque aun no tiene ningun chat activo
        cargarUsuarios(""); //Cargamos todos los usuarios

        floatButton=root.findViewById(R.id.floatingActionButtonNuevoContacto);
        imgChatbot=root.findViewById(R.id.imgChatbot);

        //Toolbar.  tenemos q configurar el toolbar que se debe mostrar en este fragmento
        toolbar=root.findViewById(R.id.chat_toolbarChatAmigos);
        constraintLayout=root.findViewById(R.id.toolbarBuscarLayout);
        comprobarColores();//Aplicamos los colores correspondientes dependiendo del tema
        toolbarSearchEditText=root.findViewById(R.id.editTextToolbarSearch);
        toolbarImageSearch=root.findViewById(R.id.imageViewToolbarBuscar);
        toolbarTitulo=root.findViewById(R.id.toolbarBuscarTitulo);
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarAjustes);
        toolbarTitulo.setText(getString(R.string.nav_chats));
        toolbarSearchEditText.setVisibility(View.INVISIBLE);
        toolbarImageSearch.setOnClickListener(new View.OnClickListener() { //Cuando se clicke en la lopa de buscar:
            @Override
            public void onClick(View v) {
                if (toolbarSearchEditText.getVisibility()==View.VISIBLE){ //Si el edit text para agregar el texto a buscar estaba visible:
                    toolbarSearchEditText.setVisibility(View.INVISIBLE);//Lo volveremos invisible
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_buscar)); //Y cambiaremos el icono de la X al icono de buscar para hacer una nueva busqueda
                    //Cerramos el teclado
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(toolbarSearchEditText.getWindowToken(), 0);
                    cargarUsuarios("");//Y cargaremos los usuarios de nuevo pero sin ninguna busqueda. si no los bvolvemos a cargar se quedaria con la ultima busqueda
                }else { //Si por el contrario el edittext esta invisible, es que acabamos de dar a la lupa para buscar
                    toolbarSearchEditText.setVisibility(View.VISIBLE);//Volveremos el edittext visible para q se pueda realizar una busqueda
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_cerrar)); //Cambiamos el icono de la lupa por una X para cerrar la busqueda
                    //Le ponemos el focus y abrimos el teclado para q escriba
                    toolbarSearchEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(toolbarSearchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() {//Cuando se clicke al icono de ajustes abriremos la ventana de ajustes
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chatsamigos_to_ajustesFragment,null,options);

            }
        });
        toolbarSearchEditText.addTextChangedListener(new TextWatcher() {//Cuando le demos a la lupa tendremos que cargar los usuarios con el texto que se ha escrito en el edittext
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarUsuarios(s.toString()); //Cargamos los usuarios que e nombre de usuario coincida con el text
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        //En este caso ponemos visible la nevagacion inferior, nos interesa que se muestre para poder viajar entre los diferentes fragmentos
        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.VISIBLE);

        floatButton.setOnClickListener(new View.OnClickListener() { //Cuando le demos al boton flotante +, se abrira el fragmento con todos los chats. se listarán todos los usuarios que estan registrados en la app para que puedas hablar con ellos
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chatsamigos_to_navigation_chattodos,null,options);
            }
        });


        imgChatbot.setOnClickListener(new View.OnClickListener() { //Cuando le das al boton flotante con el icono del chatbot se abrira el fragmento chatbot, donde se podrá mantener una conversacion con un chat virtual
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chatsamigos_to_chatbotFragment,null,options);
            }
        });


        return root;
    }
    public void cambiarVisibilidad(){ //Cuando hay minimo un chat, ocultaremos este textview, que simplemente informe que no tiene ningun chat activo
        nohaychats.setVisibility(View.INVISIBLE);
    }
    public void cargarUsuarios(String search){ //Cargamos todos los usuarios que tienen iniciada una conversacion con el usuario actual
        //Firebase nos ayuda tambien a la hora de crear los recycleViews, nos ofrece un FirebaseRecyclerOptions y FirebaseRecyclerAdapter por lo que no tenemos que crear una clase para el adaptador
        //Cogeremos todos los elementos que hay almacenados en la base de datos en la tabla Perfil. y los ordenamos por el nombredeUsuario por ahora.
        Query query= mDatabaseRef.orderByChild("nombreUsuario").startAt(search).endAt(search+"\uf8ff");
        //Creamos una clase adaptadora que será Usuario, que debera tener los mismos atributos que la tabla Perfil de la base de datos para poder unirlos correctamente
        options= new FirebaseRecyclerOptions.Builder<Usuario>().setQuery(query,Usuario.class).build();
        adapter= new FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderChatUsuarios holder, int position, @NonNull Usuario model) {
                //Por cada elemento tendremos q añadirlo al holder, pero tenemos que mirar si es nuestro perfil actual, ya que en ese caso no deberiamos mostrarlo, porque una persona no va a hablar con si mismo
                mDatabaseRefMensajes.child(mUser.getUid()).child(model.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            cambiarVisibilidad();//Si existe, ya hay chats asique ocultamos el textview ese
                            //Si existe el usuario
                            Picasso.get().load(model.getFotoPerfil()).into(holder.fotoPerfil); //Mostramos la foto de perfil
                            holder.nombreUsuario.setText(model.getNombreUsuario()); //Mostramos el nombre de uusario
                            ArrayList<Mensaje> lista= new ArrayList<Mensaje>(); //Necesitamos conseguir cual es el ultimo mensaje, añadiremos en la lista todos los emnsajes
                            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                Mensaje mensaje = postSnapshot.getValue(Mensaje.class);
                                lista.add(mensaje);
                            }
                            Log.d("Logs","Ultimo mensaje: "+lista.get(lista.size()-1).getMensaje());
                            Mensaje ultimo=lista.get(lista.size()-1); //Cogemos el ultimo mensaje
                            String msg=ultimo.getMensaje(); //Cogemos su texto
                             if(ultimo.getMensaje().contains("https://firebasestorage.googleapis.com/")) { //Si el ultimo mensaje es una imagen, en vez de mostrar la url de la imagen, pondremos el texto IMAGEN para informar que el ultimo mensaje ha sido una foto
                                msg="IMAGEN";
                            }
                            if (ultimo.getUsuario().equals(mUser.getUid())){ //Si he sido yo el que ha mandado el ultimo mensaje, pondremos por delante un Tú, para saber que ha sido nuestro
                                msg="Tú: "+msg;
                                holder.info.setTextColor(Color.parseColor("#61699C")); //Y le pondremos un color azulito al mensaje
                            }else{ //Si el mensaje no lo he mandado yo, sino el otro usuario, le cambiamos el color a uno gris
                                holder.info.setTextColor(Color.parseColor("#AAAAAA"));
                            }
                            holder.info.setText(msg); //Mostramos el mensaje
                            holder.textHoraUltimoMensaje.setText(ultimo.getHora()); //Mostramos la ultima hora del mensaje


                            //Tambien queremos mostrar cuantos mensajes hay sin leer, asique haremos los siguiente
                            mDatabaseRefMensajes.child(model.getUid()).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() { //Buscamos todos los mensajes enviados
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        int mensajesLeidos=0;
                                        for (DataSnapshot d: snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            Mensaje mensaje= d.getValue(Mensaje.class);
                                           if(mensaje.getLeido().equals("no") && mensaje.getUsuario().equals(model.getUid())){ //Cogemos los mensajes enviados por el otro usuario y que esten en leido='np'
                                                mensajesLeidos++; //Y de ser asi incrementamos el valor
                                            }
                                        }
                                        if(mensajesLeidos!=0){ //Si mensajesleidos es más que  significa que tenemos algun mensaje sin leer. por lo cual
                                            holder.textViewMensajesSinLeer.setText(""+mensajesLeidos); //En el textView de mensajes sin leer, añadiremos el numero de mensajes que tenemos sin leer
                                            holder.textHoraUltimoMensaje.setTextColor(Color.parseColor("#50DA2A"));//Ponemos el textview de la hora en color verde, para llamar mas la atencion
                                            holder.textViewMensajesSinLeer.setTextColor(Color.parseColor("#50DA2A")); //Ponemos el textview de la cantidad de mensajes sin leer a verde
                                            holder.info.setTextColor(Color.parseColor("#50DA2A")); //Y ponemos el ultimo mensaje en verde tambn

                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                    }
                                });

                            //Tendremos que mirar si los otros usuarios se encuentran actualmente conectados a la app o desconectados
                            mDatabaseRef.child(model.getUid()).addListenerForSingleValueEvent(new ValueEventListener() { //Para ello buscamos el perfil del otro usuario
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        //Si existe el usuario
                                        String conectado=snapshot.child("conectado").getValue().toString(); //Cogemos su valor del parametro conectado
                                        if ("Conectado".equals(conectado)){
                                            holder.icRojo.setVisibility(View.INVISIBLE);
                                        }else{
                                            holder.icVerde.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });

                        }else{
                            holder.itemView.setVisibility(View.GONE); //si somos nosotros el elemento que toca, debemos omitir la lista y pasar al siguiente elemento
                            ViewGroup.LayoutParams params=holder.itemView.getLayoutParams();
                            params.height= 0;
                            holder.itemView.setLayoutParams(params);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                holder.itemView.setOnClickListener(new View.OnClickListener(){
                    //Cuando clickemos encima de un usuario nos deberá abrir el chat con dicha persona
                    @Override
                    public void onClick(View view){
                        //Abrimos el chat, y le pasamos el codigo de la persona con la que vamos a hablar

                        Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                        bundle.putString("usuario", getRef(position).getKey().toString());
                        NavOptions options = new NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .build();
                        Navigation.findNavController(view).navigate(R.id.action_navigation_chatsamigos_to_chatFragment, bundle,options);


                    }
                });
            }

            @NonNull
            @Override
            public ViewHolderChatUsuarios onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_usuario,parent,false);
                return new ViewHolderChatUsuarios(view);
            }
        };

        adapter.startListening();
        recyclerView.setAdapter(adapter);

    }

    public void comprobarColores(){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String tema = "";
        if (prefs.contains("tema")) {
            tema = prefs.getString("tema", null);
        }
        switch (tema) {
            case "morado":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_morado,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.toolbar_morado_claro)));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_morado,getContext().getTheme()));
                break;
            case "naranja":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.toolbar_naranja_claro)));
                break;
            case "verde":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.toolbar_verde_claro)));
                break;
            case "azul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.azul_clarito)));
                break;
            case "verdeazul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.toolbar_verdeazul_claro)));
                break;
            default:
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                imgChatbot.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.azul_clarito)));
                break;
        }
    }

}