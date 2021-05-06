package com.example.nerechat.ui.chats;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import com.example.nerechat.helpClass.Usuario;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
    FloatingActionButton floatButton;
    TextView nohaychats;

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
                mDatabaseRefMensajes.child(mUser.getUid()).child(model.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            cambiarVisibilidad();
                            //Si existe el usuario
                            Picasso.get().load(model.getFotoPerfil()).into(holder.fotoPerfil); //Mostramos la foto de perfil
                            holder.nombreUsuario.setText(model.getNombreUsuario()); //Mostramos el nombre de uusario
                            holder.info.setText(model.getEstado()); //Mostramos la info
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
                        abrirChat(getRef(position).getKey().toString());
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

    public void abrirChat(String usu){
        //Abrimos el chat y le pasamos el extra del id del usuario con el que vamos a hablar
        Intent i = new Intent(getContext(), ChatActivity.class);
        i.putExtra("usuario", usu);
        startActivity(i);
    }


}