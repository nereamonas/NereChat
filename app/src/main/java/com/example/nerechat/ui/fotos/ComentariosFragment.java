package com.example.nerechat.ui.fotos;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
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
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderComment;
import com.example.nerechat.base.BaseFragment;
import com.example.nerechat.helpClass.Comment;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ComentariosFragment extends BaseFragment {

    String idPost;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRefPerfil, mDatabaseRefImagenes, mDatabaseRefComentarios;
    FirebaseRecyclerOptions<Comment> options;
    FirebaseRecyclerAdapter<Comment, ViewHolderComment> adapter;

    RecyclerView recyclerView;
    DividerItemDecoration dividerItemDecoration;

    ImageView toolbarImagenAjustes;
    TextView toolbarTitulo;
    Toolbar toolbar;
    ConstraintLayout constraintLayout;

    EditText editTextComment;
    ImageView sendComment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_comentarios, container, false);

        idPost = getArguments().getString("idPost");

        toolbar=root.findViewById(R.id.chat_toolbarComentarios);
        constraintLayout=root.findViewById(R.id.toolbatPrincipalLayout);
        comprobarColores(); //Aplicamos los colores correspondientes dependiendo del tema
        toolbarTitulo = root.findViewById(R.id.toolbarPrincipalTitulo);
        toolbarTitulo.setText(getString(R.string.nav_comentarios));
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarPrincipalAjustes);
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() { //Cuando se clique en el boton  de ajustes del toolvar, navegaremos al fragment de ajustes
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_comentariosFragment_to_ajustesFragment,null,options);

            }
        });

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRefPerfil= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos donde se encuentras los perfiles de usuario
        mDatabaseRefImagenes= FirebaseDatabase.getInstance().getReference().child("Imagen"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes
        mDatabaseRefComentarios= FirebaseDatabase.getInstance().getReference().child("Comentarios");

        recyclerView=root.findViewById(R.id.recyclerComentarios);
        LinearLayoutManager llm=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        dividerItemDecoration= new DividerItemDecoration(getContext(),llm.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        editTextComment = root.findViewById(R.id.editTextComment);
        sendComment = root.findViewById(R.id.sendComment);

        sendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comentar();
            }
        });

        cargarComentarios();


        return root;
    }

    private void cargarComentarios(){

        Query query= mDatabaseRefComentarios.child(idPost).orderByChild("hora").startAt("").endAt("\uf8ff"); //Cargar todos los elementos de Imagen
        options= new FirebaseRecyclerOptions.Builder<Comment>().setQuery(query,Comment.class).build();
        adapter= new FirebaseRecyclerAdapter<Comment, ViewHolderComment>(options) {

            @NonNull
            @Override
            public ViewHolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_comentarios,parent,false);
                return new ViewHolderComment(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ViewHolderComment holder, int position, @NonNull Comment model) {
                holder.comment.setText(model.getTexto());
                holder.hora.setText(model.getHora());

                mDatabaseRefPerfil.child(model.getUsername()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Picasso.get().load(snapshot.child("fotoPerfil").getValue().toString()).into(holder.fotoPerfil);
                            String miNombreUsuario = snapshot.child("nombreUsuario").getValue().toString();
                            holder.username.setText(miNombreUsuario);
                        }
                        mDatabaseRefPerfil.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

                holder.comment.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (model.getUsername().equals(mUser.getUid())) {
                            AlertDialog.Builder dialogo = new AlertDialog.Builder(v.getContext());
                            dialogo.setTitle(getString(R.string.alerta_Eliminarcomentario));
                            dialogo.setMessage(getString(R.string.alerta_Quiereseliminarelcomentario) + " '" + model.getTexto() + "'?");

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
                                        Toast.makeText(getContext(), getString(R.string.toast_haseliminadoelcomentario), Toast.LENGTH_SHORT).show();
                                    }
                                    mDatabaseRefComentarios.child(idPost).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot d : snapshot.getChildren()) { //Por cada datasnapshot (es decir cada foto subida a firebase)
                                                Comment comment = d.getValue(Comment.class); //Cogemos el mensaje
                                                //Si el mensaje es igual al que queremos borrar
                                                if (comment.getUsername().equals(model.getUsername()) && comment.getHora().equals(model.getHora()) && comment.getTexto().equals(model.getTexto())) {
                                                    mDatabaseRefComentarios.child(idPost).child(d.getKey()).removeValue(); //eliminamos el mensaje
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                        }
                                    }); //Eliminar mensaje mi usuario - el otro
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
                        return true;
                    }
                });
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }


    public void comentar() { //Mandar un mensaje. ponemos uri como parametro  y cuando se envie una foto este parametro ira informado
        //Mandar un mensaje.
        String comment = editTextComment.getText().toString(); //Cogemos el valor del mensaje
        if ((!comment.equals(""))) { //El mensaje debe ser distinto de "", o la uri debe ser distinto de null sino no se mandara
            Date date = new Date(); //Cargamos la hora exacta en la que estamos para enviar el mensaje
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            editTextComment.setText(""); //Actualizamos el valor de mensaje a "" para q el usuario escriba otro mensaje

            //Creamos un hashMap que subiremos a la base de datos en la tabla MensajesChat, y guardaremos el mensaje enviado y el Uid del usuario que lo ha mandado, en este caso yo
            HashMap hashMap = new HashMap();
            hashMap.put("texto", comment); //ponemos el mensaje normal
            hashMap.put("username", mUser.getUid()); //ponemos el uid del usuario
            hashMap.put("hora", formatter.format(date)); //la hora
            //Tenemos que hacer dos cosas, por un lado, subirlo con el titulo de mi usuario y subtitulo del otro usuario y por otro lado con el titulo del otro usuario y subtitulo de mi usuario, para tener las referencias con las dos personas
            mDatabaseRefComentarios.child(idPost).push().updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //Primero subido corectamente
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext()); //Cogemos las preferencias
                    boolean activadas=false;
                    if (prefs.contains("notiftoast")) { //Comprobamos si existe notif
                        activadas = prefs.getBoolean("notiftoast", true);  //Comprobamos si las notificaciones estan activadas
                    }
                    if(activadas)
                        Toast.makeText(getContext(), getString(R.string.toast_comentado), Toast.LENGTH_SHORT).show();
                }
            });

        }
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