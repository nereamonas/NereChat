package com.example.nerechat.ui.fotos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.nerechat.R;
import com.example.nerechat.base.BaseViewModel;

public class FotosFragment extends Fragment {

    private BaseViewModel fotosViewModel;

    ImageView toolbarImagenAjustes;
    TextView toolbarTitulo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        fotosViewModel =
                new ViewModelProvider(this).get(BaseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_fotos, container, false);

        toolbarImagenAjustes=root.findViewById(R.id.imageViewToolbarPrincipalAjustes);
        toolbarTitulo=root.findViewById(R.id.toolbarPrincipalTitulo);
        toolbarTitulo.setText(getString(R.string.nav_fotos));
        toolbarImagenAjustes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(v).navigate(R.id.action_navigation_fotos_to_ajustesFragment,null,options);

            }
        });


        return root;
    }
}