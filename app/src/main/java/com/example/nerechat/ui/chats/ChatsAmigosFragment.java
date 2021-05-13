package com.example.nerechat.ui.chats;

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
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
        nohaychats.setVisibility(View.VISIBLE);
        cargarUsuarios("");


        //Toolbar
        toolbarSearchEditText=root.findViewById(R.id.editTextToolbarSearch);
        toolbarImageSearch=root.findViewById(R.id.imageViewToolbarBuscar);
        toolbarTitulo=root.findViewById(R.id.toolbarBuscarTitulo);
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarAjustes);
        toolbarTitulo.setText(getString(R.string.nav_chats));
        toolbarSearchEditText.setVisibility(View.INVISIBLE);
        toolbarImageSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toolbarSearchEditText.getVisibility()==View.VISIBLE){
                    toolbarSearchEditText.setVisibility(View.INVISIBLE);
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_buscar));
                    //Cerramos el teclado
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(toolbarSearchEditText.getWindowToken(), 0);
                    cargarUsuarios("");
                }else {
                    toolbarSearchEditText.setVisibility(View.VISIBLE);
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_cerrar));
                    //Le ponemos el focus y abrimos el teclado para q escriba
                    toolbarSearchEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(toolbarSearchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chatsamigos_to_ajustesFragment,null,options);

            }
        });
        toolbarSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarUsuarios(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });


        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.VISIBLE);

        floatButton=root.findViewById(R.id.floatingActionButtonNuevoContacto);
        floatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chatsamigos_to_navigation_chattodos,null,options);
            }
        });

        imgChatbot=root.findViewById(R.id.imgChatbot);

        imgChatbot.setOnClickListener(new View.OnClickListener() {
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
    public void cambiarVisibilidad(){
        nohaychats.setVisibility(View.INVISIBLE);
    }
    public void cargarUsuarios(String search){
        //Firebase nos ayuda tambien a la hora de crear los recycleViews, nos ofrece un FirebaseRecyclerOptions y FirebaseRecyclerAdapter por lo que no tenemos que crear una clase para el adaptador
        //Cogeremos todos los elementos que hay almacenados en la base de datos en la tabla Perfil. y los ordenamos por el nombredeUsuario por ahora.
        Query query= mDatabaseRef.orderByChild("nombreUsuario").startAt(search).endAt(search+"\uf8ff");
        //Creamos una clase adaptadora que será Usuario, que debera tener los mismos atributos que la tabla Perfil de la base de datos para poder unirlos correctamente
        options= new FirebaseRecyclerOptions.Builder<Usuario>().setQuery(query,Usuario.class).build();
        adapter= new FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderChatUsuarios holder, int position, @NonNull Usuario model) {
                //Por cada elemento tendremos q añadirlo al holder, pero tenemos que mirar si es nuestro perfil actual, ya que en ese caso no deberiamos mostrarlo, porque una persona no va a hablar con si mismo
                mDatabaseRefMensajes.child(mUser.getUid()).child(model.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            cambiarVisibilidad();
                            //Si existe el usuario
                            Picasso.get().load(model.getFotoPerfil()).into(holder.fotoPerfil); //Mostramos la foto de perfil
                            holder.nombreUsuario.setText(model.getNombreUsuario()); //Mostramos el nombre de uusario
                            ArrayList<Mensaje> lista= new ArrayList<Mensaje>();
                            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                                Mensaje mensaje = postSnapshot.getValue(Mensaje.class);
                                lista.add(mensaje);
                            }
                            Log.d("Logs","Ultimo mensaje: "+lista.get(lista.size()-1).getMensaje());
                            Mensaje ultimo=lista.get(lista.size()-1);
                            String msg=ultimo.getMensaje();
                            if (ultimo.getUsuario().equals(mUser.getUid())){
                                msg="Tú: "+ultimo.getMensaje();
                                holder.info.setTextColor(getContext().getResources().getColor(R.color.azul_oscuro));
                            }
                            holder.info.setText(msg); //Mostramos la info
                            holder.textHoraUltimoMensaje.setText(ultimo.getHora());


                            mDatabaseRef.child(model.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        //Si existe el usuario
                                        String conectado=snapshot.child("conectado").getValue().toString();
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
                            holder.itemView.setVisibility(View.GONE);
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

}