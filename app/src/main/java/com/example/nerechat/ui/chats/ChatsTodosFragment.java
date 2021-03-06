package com.example.nerechat.ui.chats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nerechat.R;
import com.example.nerechat.adaptadores.RecyclerViewAdapterChatUsuarios.ViewHolderChatUsuarios;
import com.example.nerechat.base.BaseViewModel;
import com.example.nerechat.helpClass.Usuario;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

public class ChatsTodosFragment extends Fragment {
    private BaseViewModel chatsTodosViewModel;
    //Se muestra un recyclerview + card view de todos los perfiles con los que podemos hablar

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;
    FirebaseRecyclerOptions<Usuario> options;
    FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios> adapter;

    RecyclerView recyclerView;
    DividerItemDecoration dividerItemDecoration;

    EditText toolbarSearchEditText;
    ImageView toolbarImageSearch,toolbarImagenAjustes;
    TextView toolbarTitulo;
    Toolbar toolbar;
    ConstraintLayout constraintLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        chatsTodosViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_chats_todos, container, false);


        //Inicializamos las variables
        mAuth= FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //Referencia a la base de datos donde se encuentras los perfiles de usuario

        recyclerView=root.findViewById(R.id.recycleViewChatTodos);
        LinearLayoutManager llm=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        dividerItemDecoration= new DividerItemDecoration(getContext(),llm.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        cargarUsuarios("");

        //En este caso desactivaremos la nevagacion inferior, no nos interesa que se muestre
        BottomNavigationView nv=  ((AppCompatActivity)getActivity()).findViewById(R.id.nav_view);
        nv.setVisibility(View.GONE);

        //Toolbar. tenemos q configurar el toolbar que se debe mostrar en este fragmento
        toolbar=root.findViewById(R.id.chat_toolbarChatTodos);
        constraintLayout=root.findViewById(R.id.toolbarBuscarLayout);
        comprobarColores(); //Aplicamos los colores correspondientes dependiendo del tema
        toolbarSearchEditText=root.findViewById(R.id.editTextToolbarSearch);
        toolbarImageSearch=root.findViewById(R.id.imageViewToolbarBuscar);
        toolbarTitulo=root.findViewById(R.id.toolbarBuscarTitulo);
        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarAjustes);
        toolbarTitulo.setText(getString(R.string.nav_todoslosusuarios));
        toolbarSearchEditText.setVisibility(View.INVISIBLE);
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

        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() { //Cuando se clicke al icono de ajustes abriremos la ventana de ajustes
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_chattodos_to_ajustesFragment,null,options);

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


    public void cargarUsuarios(String search){
        //Firebase nos ayuda tambien a la hora de crear los recycleViews, nos ofrece un FirebaseRecyclerOptions y FirebaseRecyclerAdapter por lo que no tenemos que crear una clase para el adaptador
        //Cogeremos todos los elementos que hay almacenados en la base de datos en la tabla Perfil. y los ordenamos por el nombredeUsuario por ahora.
        Query query= mDatabaseRef.orderByChild("nombreUsuario").startAt(search).endAt(search+"\uf8ff");
        //Creamos una clase adaptadora que ser?? Usuario, que debera tener los mismos atributos que la tabla Perfil de la base de datos para poder unirlos correctamente
        options= new FirebaseRecyclerOptions.Builder<Usuario>().setQuery(query,Usuario.class).build();
        adapter= new FirebaseRecyclerAdapter<Usuario, ViewHolderChatUsuarios>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewHolderChatUsuarios holder, int position, @NonNull Usuario model) {
                //Por cada elemento tendremos q a??adirlo al holder, pero tenemos que mirar si es nuestro perfil actual, ya que en ese caso no deberiamos mostrarlo, porque una persona no va a hablar con si mismo
                if(!mUser.getUid().equals(getRef(position).getKey().toString())){
                    Picasso.get().load(model.getFotoPerfil()).into(holder.fotoPerfil); //Mostramos la foto de perfil
                    holder.nombreUsuario.setText(model.getNombreUsuario()); //Mostramos el nombre de uusario
                    holder.info.setText(""); //Mostramos la info
                }else{ //Es nuestro perfil actual, por lo que tenemos q omitir este elemento
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                }

                holder.itemView.setOnClickListener(new View.OnClickListener(){
                    //Cuando clickemos encima de un usuario nos deber?? abrir el chat con dicha persona
                    @Override
                    public void onClick(View view){
                        //Abrimos el chat, y le pasamos el codigo de la persona con la que vamos a hablar
                        Bundle bundle = new Bundle(); //Con el bundle podemos pasar datos
                        bundle.putString("usuario", getRef(position).getKey().toString());
                        NavOptions options = new NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .build();
                        Navigation.findNavController(view).navigate(R.id.action_navigation_chattodos_to_chatFragment, bundle,options);

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