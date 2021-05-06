package com.example.nerechat.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.nerechat.IniciarSesionActivity;
import com.example.nerechat.R;
import com.example.nerechat.base.BaseViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class PerfilFragment extends Fragment {

    FirebaseAuth mAuth;
    private BaseViewModel perfilViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        perfilViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);


        Button b=root.findViewById(R.id.buttonLogOut);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent i = new Intent(getContext(), IniciarSesionActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            }
        });

        mAuth= FirebaseAuth.getInstance();

        return root;
    }

}