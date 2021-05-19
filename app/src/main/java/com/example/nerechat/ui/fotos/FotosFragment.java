package com.example.nerechat.ui.fotos;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nerechat.R;
import com.example.nerechat.SubirFotoActivity;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderImagen;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Imagen;
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


public class FotosFragment extends Fragment {

    private BaseViewModel fotosViewModel;


    ImageView toolbarImagenAjustes;
    TextView toolbarTitulo;
    FloatingActionButton floatButton;
    RecyclerView recyclerView;
    DividerItemDecoration dividerItemDecoration;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRefPerfil,mDatabaseRefImagenes, mDatabaseRefLikes;
    FirebaseRecyclerOptions<Imagen> options;
    FirebaseRecyclerAdapter<Imagen, ViewHolderImagen> adapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        fotosViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_fotos, container, false);

        toolbarImagenAjustes = root.findViewById(R.id.imageViewToolbarAjustes);
        toolbarTitulo = root.findViewById(R.id.toolbarBuscarTitulo);
        toolbarTitulo.setText(getString(R.string.nav_fotos));

        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRefPerfil= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos donde se encuentras los perfiles de usuario
        mDatabaseRefImagenes= FirebaseDatabase.getInstance().getReference().child("Imagen"); //Y la base de datos mensajeChat donde se almacenarán todos los mensajes
        mDatabaseRefLikes= FirebaseDatabase.getInstance().getReference().child("Likes");

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
                Intent i = new Intent(getContext(), SubirFotoActivity.class);
                startActivity(i);
            }
        });

        return root;
    }

    private void cargarFotos() {
        Query query= mDatabaseRefImagenes.orderByChild("descripcion").startAt("").endAt("\uf8ff"); //Cargar todos los elementos de Imagen
        options= new FirebaseRecyclerOptions.Builder<Imagen>().setQuery(query,Imagen.class).build();
        adapter= new FirebaseRecyclerAdapter<Imagen, ViewHolderImagen>(options) {

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
                            holder.descripcion.setText(model.getDescripcion());
                            holder.likes.setText(model.getLikes()+ " likes");
                            holder.mg2.setVisibility(View.INVISIBLE);
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
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }


}