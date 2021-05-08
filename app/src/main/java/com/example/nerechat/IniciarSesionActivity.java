package com.example.nerechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nerechat.base.BaseActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class IniciarSesionActivity extends BaseActivity {
    //Pagina de inicio de sesion

    TextInputLayout inputemail, inputpass;

    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    FirebaseUser mUser;
    DatabaseReference mDatabaseRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iniciar_sesion);

        //Cargamos las referencias
        inputemail=findViewById(R.id.inputLEmail);
        inputpass=findViewById(R.id.inputLPass);

        mAuth=FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser(); //El usuario actual que tiene iniciada la sesion
        mDatabaseRef= FirebaseDatabase.getInstance().getReference().child("Perfil"); //para comprobar el perfil del usuario correspondiente
        progressDialog=new ProgressDialog(this);

        TextView text_crearCuenta= findViewById(R.id.text_crearCuenta); //Crear cuenta
        text_crearCuenta.setOnClickListener(new View.OnClickListener() {  //Cuando clickemos en el boton camara. pediremos permisos para abrir la camara y guardar las fotos
            @Override
            public void onClick(View v) {
                abrirCrearCuenta();
            }
        });

    }

    public void abrirCrearCuenta(){ //Abrimos la pantalla de crear cuenta
        Intent i = new Intent(this, CrearCuentaActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }


    public void abrirPrincipal(){ //Abrimos la pantalla principal porque ya se ha autenticado correctamente
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    public void abrirCrearPerfil(){ //Abrimos la ventana de que falta crear el perfil de usuario
        Intent i = new Intent(this, CrearPerfilActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    public void iniciarSesion(View v){ //Click en iniciar sesion
        //Cogemos los correspondientes valores del input. el email y la pass
        String email=inputemail.getEditText().getText().toString();
        String pass=inputpass.getEditText().getText().toString();

        if (email.equals("")||!email.contains("@")) { //Pasa que el email sea valido tiene q ser !="" y contener un @. Si no es asi saltamos un error
            inputemail.setError("El email no es correcto"); //Los tipo input nos permiten mostrar el error directamente, lo que viene muy bien para los inicio de sesion
            inputemail.requestFocus();
        }else if(pass.equals("")||pass.length()<6){ //La contraseña tiene que tener un minimo de 6 caracteres (Es una restriccion de las pass de firebase). Si no se cumple mostramos mensaje de error
            inputpass.setError("La contraseña tiene que tener un minimo de 6 caracteres");
            inputpass.requestFocus();
        }else{//Los datos son correctos asique procedemos a iniciar sesion
            //Mostramos el dialog bar. Que se mantendrá activo hasta que se complete el inicioi de sesion
            progressDialog.setTitle("Iniciando sesion...");
            progressDialog.setMessage("Porfavor espera");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            mAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        Log.d("Logs", "Success sign in ");
                        //Se ha registrado correctamente
                        mUser=mAuth.getCurrentUser();
                        abrirPrincipal();
                        progressDialog.dismiss(); //Cancelamos la barra de proceso

                    }else{
                        Log.d("Logs", "No se ha podido sign in ");
                        progressDialog.dismiss();
                        //Mostrar alerta de q no se ha podido registrar
                        Toast.makeText(IniciarSesionActivity.this,"No se ha podido iniciar sesion. Revisa los datos",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }
}