package com.example.nerechat.ui.fotos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderChatUsuarios;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderImagen;
import com.example.nerechat.base.BaseFragment;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Imagen;
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

import java.util.HashMap;


public class FotosFragment extends BaseFragment {

    private BaseViewModel fotosViewModel;

    EditText toolbarSearchEditText;
    ImageView toolbarImagenAjustes,toolbarImageSearch;
    TextView toolbarTitulo;
    FloatingActionButton floatButton;
    RecyclerView recyclerView;
    DividerItemDecoration dividerItemDecoration;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRefPerfil,mDatabaseRefImagenes, mDatabaseRefLikes,mDatabaseRefComentarios;
    FirebaseRecyclerOptions<Imagen> options_imagen;
    FirebaseRecyclerAdapter<Imagen, ViewHolderImagen> adapter_imagen;
    FirebaseRecyclerOptions<Usuario> options_user;
    FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios> adapter_user;

    Toolbar toolbar;
    ConstraintLayout constraintLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        fotosViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_fotos, container, false);

        toolbar=root.findViewById(R.id.chat_toolbarFotos);
        constraintLayout=root.findViewById(R.id.cltoolbarFotos);
        comprobarColores(); //Aplicamos los colores correspondientes dependiendo del tema
        toolbarImagenAjustes = root.findViewById(R.id.imageViewToolbarAjustes);
        toolbarTitulo = root.findViewById(R.id.toolbarBuscarTitulo);
        toolbarTitulo.setText(getString(R.string.nav_fotos));
        toolbarSearchEditText=root.findViewById(R.id.editTextToolbarSearch);
        toolbarImageSearch=root.findViewById(R.id.imageViewToolbarBuscar);
        toolbarSearchEditText.setVisibility(View.INVISIBLE);

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRefPerfil= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos donde se encuentras los perfiles de usuario
        mDatabaseRefImagenes= FirebaseDatabase.getInstance().getReference().child("Imagen"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes
        mDatabaseRefLikes= FirebaseDatabase.getInstance().getReference().child("Likes");
        mDatabaseRefComentarios= FirebaseDatabase.getInstance().getReference().child("Comentarios");

        recyclerView=root.findViewById(R.id.recyclerFotos);
        LinearLayoutManager llm=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        dividerItemDecoration= new DividerItemDecoration(getContext(),llm.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        cargarFotos();

        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_fotos_to_ajustesFragment, null, options);

            }
        });

        floatButton = root.findViewById(R.id.floatingActionButtonSubirFoto);
        floatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_fotos_to_subirFotoFragment, null, options);
            }
        });

        toolbarImageSearch.setOnClickListener(new View.OnClickListener() { //Cuando se clicke en la lopa de buscar:
            @Override
            public void onClick(View v) {
                if (toolbarSearchEditText.getVisibility()==View.VISIBLE){ //Si el edit text para agregar el texto a buscar estaba visible:
                    toolbarSearchEditText.setVisibility(View.INVISIBLE); //Lo volveremos invisible
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_buscar)); //Y cambiaremos el icono de la X al icono de buscar para hacer una nueva busqueda
                    //Cerramos el teclado, ya que se ha terminado con la accion de buscar
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(toolbarSearchEditText.getWindowToken(), 0);
                    cargarUsuarios(""); //Y cargaremos los usuarios de nuevo pero sin ninguna busqueda. si no los bvolvemos a cargar se quedaria con la ultima busqueda
                }else { //Si por el contrario el edittext esta invisible, es que acabamos de dar a la lupa para buscar
                    toolbarSearchEditText.setVisibility(View.VISIBLE); //Volveremos el edittext visible para q se pueda realizar una busqueda
                    toolbarImageSearch.setImageDrawable(getResources().getDrawable(R.drawable.ic_cerrar)); //Cambiamos el icono de la lupa por una X para cerrar la busqueda
                    //Le ponemos el focus y abrimos el teclado para q escriba
                    toolbarSearchEditText.requestFocus();
                    InputMethodManager imm = (InputMethodManager) ((AppCompatActivity)getActivity()).getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(toolbarSearchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        toolbarSearchEditText.addTextChangedListener(new TextWatcher() { //Cuando le demos a la lupa tendremos que cargar los usuarios con el texto que se ha escrito en el edittext
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                cargarUsuarios(s.toString()); //Cargamos los usuarios que e nombre de usuario coincida con el text
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        return root;
    }

    private void cargarFotos() {
        Query query= mDatabaseRefImagenes.orderByChild("descripcion").startAt("").endAt("\uf8ff"); //Cargar todos los elementos de Imagen
        options_imagen = new FirebaseRecyclerOptions.Builder<Imagen>().setQuery(query,Imagen.class).build();
        adapter_imagen = new FirebaseRecyclerAdapter<Imagen, ViewHolderImagen>(options_imagen) {

            @NonNull
            @Override
            public ViewHolderImagen onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.foto_item,parent,false);
                return new ViewHolderImagen(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ViewHolderImagen holder, int position, @NonNull Imagen model) {
                mDatabaseRefImagenes.addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){

                            //Picasso.get().load(model.getUri()).into(holder.fotoPost);
                            Glide.with(getContext()).load(model.getUri()).into(holder.fotoPost);

                            //holder.mg.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_feliz));
                            //holder.mg2.setImageDrawable(getContext().getResources().getDrawable(R.mipmap.ic_like));

                            holder.descripcion.setText(model.getDescripcion());
                            holder.likes.setText(model.getLikes()+ " likes");
                            holder.mg2.setVisibility(View.INVISIBLE);
                            if(model.getUid().equals(mUser.getUid())){
                                holder.borrar.setVisibility(View.VISIBLE);
                            }else{
                                holder.borrar.setVisibility(View.GONE);
                            }

                            mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        //Por cada datasnapshot (es decir cada foto subida a firebase)
                                        if (snapshot.getKey().equals(mUser.getUid()) && snapshot.exists()) {
                                            holder.mg2.setVisibility(View.VISIBLE);
                                        }
                                    }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            mDatabaseRefComentarios.child(model.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    int cantidadComent=0;
                                    for(DataSnapshot d:snapshot.getChildren()){
                                        cantidadComent++;
                                    }

                                    holder.coments.setText(cantidadComent+" comentarios");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            mDatabaseRefPerfil.child(model.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        Picasso.get().load(snapshot.child("fotoPerfil").getValue().toString()).into(holder.fotoPerfil);
                                        String miNombreUsuario = snapshot.child("nombreUsuario").getValue().toString();
                                        holder.usuario.setText(miNombreUsuario);
                                        holder.usuario2.setText(miNombreUsuario);
                                    }
                                    mDatabaseRefPerfil.removeEventListener(this);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });

                            holder.mg.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //CAMBIAR LA FOTO
                                    mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            //if(snapshot.exists()){
                                            Log.d("LOGS",snapshot.getKey());
                                            Log.d("LOGS",snapshot.toString());
                                                if (snapshot.getKey().equals(mUser.getUid())&&snapshot.exists()) {
                                                    //Quitar like
                                                    Log.d("LOGS",snapshot.getValue().toString());
                                                    holder.mg2.setVisibility(View.INVISIBLE);
                                                    mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).removeValue();
                                                    mDatabaseRefImagenes.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                Imagen imagen = d.getValue(Imagen.class);
                                                                if (imagen.getId().equals(model.getId())) {
                                                                    int like = Integer.parseInt(imagen.getLikes());
                                                                    like--;
                                                                    mDatabaseRefImagenes.child(model.getId()).child("likes").setValue("" + like);
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                                } else {
                                                    holder.mg2.setVisibility(View.VISIBLE);
                                                    HashMap hm = new HashMap();
                                                    hm.put("uid",mUser.getUid());
                                                    mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).updateChildren(hm);
                                                    mDatabaseRefImagenes.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                                Imagen imagen = d.getValue(Imagen.class);
                                                                if (imagen.getId().equals(model.getId())) {
                                                                    int like = Integer.parseInt(imagen.getLikes());
                                                                    like++;
                                                                    mDatabaseRefImagenes.child(model.getId()).child("likes").setValue("" + like);
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                                }
                                            }


                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            });

                            holder.mg2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //CAMBIAR LA FOTO
                                    mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            //Por cada datasnapshot (es decir cada foto subida a firebase)
                                            //if(snapshot.exists()){
                                            Log.d("LOGS",snapshot.getKey());
                                            Log.d("LOGS",snapshot.toString());
                                            if (snapshot.getKey().equals(mUser.getUid())&&snapshot.exists()) {
                                                //Quitar like
                                                Log.d("LOGS",snapshot.getValue().toString());
                                                holder.mg2.setVisibility(View.INVISIBLE);
                                                mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).removeValue();
                                                mDatabaseRefImagenes.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Imagen imagen = d.getValue(Imagen.class);
                                                            if (imagen.getId().equals(model.getId())) {
                                                                int like = Integer.parseInt(imagen.getLikes());
                                                                like--;
                                                                mDatabaseRefImagenes.child(model.getId()).child("likes").setValue("" + like);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            } else {
                                                holder.mg2.setVisibility(View.VISIBLE);
                                                HashMap hm = new HashMap();
                                                hm.put("uid",mUser.getUid());
                                                mDatabaseRefLikes.child(model.getId()).child(mUser.getUid()).updateChildren(hm);
                                                mDatabaseRefImagenes.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                            Imagen imagen = d.getValue(Imagen.class);
                                                            if (imagen.getId().equals(model.getId())) {
                                                                int like = Integer.parseInt(imagen.getLikes());
                                                                like++;
                                                                mDatabaseRefImagenes.child(model.getId()).child("likes").setValue("" + like);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });

                                            }
                                        }


                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });


                                }
                            });
                            holder.comentar.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                                    bundle.putString("idPost", model.getId());
                                    NavOptions options = new NavOptions.Builder()
                                            .setLaunchSingleTop(true)
                                            .build();
                                    Navigation.findNavController(v).navigate(R.id.action_navigation_fotos_to_comentariosFragment, bundle,options);
                                }
                            });

                            holder.borrar.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext());
                                    dialogo.setTitle(getString(R.string.alerta_Eliminarpost));
                                    dialogo.setMessage(getString(R.string.alerta_Quiereseliminarelpost) +"?");

                                    dialogo.setPositiveButton(getString(R.string.si), new DialogInterface.OnClickListener() {  //Botón si. es decir, queremos eliminar la rutina
                                        public void onClick(DialogInterface dialogo1, int id) {
                                            //Si dice que si quiere eliminar. Actualizamos la lista y lo borramos de la base de datos
                                            //Cogemos el id del elemento seleccionado por el usuario
                                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
                                            boolean activadas = false;
                                            if (prefs.contains("notiftoast")) { //Comprobamos si existe notif
                                                activadas = prefs.getBoolean("notiftoast", true);  //Comprobamos si las notificaciones estan activadas
                                            }
                                            if (activadas) {
                                                Toast.makeText(getContext(), getString(R.string.toast_haseliminadolaimagen), Toast.LENGTH_SHORT).show();
                                            }
                                            mDatabaseRefImagenes.child(model.getId()).removeValue(); //eliminamos el mensaje
                                            Log.d("Logs", "mensaje eliminado");
                                        }
                                    });

                                    //En el caso de que el usuario diga que no quiere borrarlo, pues no hará nada. se cerrará el dialogo
                                    dialogo.setNeutralButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialogo1, int id) {
                                            Log.d("Logs", "no se eliminara el mensaje");
                                        }
                                    });
                                    dialogo.show();


                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        };
        adapter_imagen.startListening();
        recyclerView.setAdapter(adapter_imagen);
    }


    public void cargarUsuarios(String search){
        //Firebase nos ayuda tambien a la hora de crear los recycleViews, nos ofrece un FirebaseRecyclerOptions y FirebaseRecyclerAdapter por lo que no tenemos que crear una clase para el adaptador
        //Cogeremos todos los elementos que hay almacenados en la base de datos en la tabla Perfil. y los ordenamos por el nombredeUsuario por ahora.
        Query query= mDatabaseRefPerfil.orderByChild("nombreUsuario").startAt(search).endAt(search+"\uf8ff");
        //Creamos una clase adaptadora que será Usuario, que debera tener los mismos atributos que la tabla Perfil de la base de datos para poder unirlos correctamente
        options_user = new FirebaseRecyclerOptions.Builder<Usuario>().setQuery(query,Usuario.class).build();
        adapter_user = new FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios>(options_user) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderChatUsuarios holder, int position, @NonNull Usuario model) {
                //Por cada elemento tendremos q añadirlo al holder, pero tenemos que mirar si es nuestro perfil actual, ya que en ese caso no deberiamos mostrarlo, porque una persona no va a hablar con si mismo
                if(!mUser.getUid().equals(getRef(position).getKey().toString())){
                    Picasso.get().load(model.getFotoPerfil()).into(holder.fotoPerfil); //Mostramos la foto de perfil
                    holder.nombreUsuario.setText(model.getNombreUsuario()); //Mostramos el nombre de uusario
                    holder.info.setText(""); //Mostramos la info
                }else{ //Es nuestro perfil actual, por lo que tenemos q omitir este elemento
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                }

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
                        Navigation.findNavController(view).navigate(R.id.action_navigation_fotos_to_perfilOtroUsFragment, bundle,options);

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

        adapter_user.startListening();
        recyclerView.setAdapter(adapter_user);

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
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_morado,getContext().getTheme()));
                break;
            case "naranja":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_naranja,getContext().getTheme()));
                break;
            case "verde":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verde,getContext().getTheme()));
                break;
            case "azul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                break;
            case "verdeazul":
                toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.toolbar_verdeazul,getContext().getTheme()));
                break;
            default:
                toolbar.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                constraintLayout.setBackgroundColor(getResources().getColor(R.color.azul_medioOscuro,getContext().getTheme()));
                break;
        }
    }


}