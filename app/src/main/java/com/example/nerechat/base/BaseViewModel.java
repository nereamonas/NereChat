package com.example.nerechat.base;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BaseViewModel extends ViewModel {
//Este metodo al crear el menu, se añadia. Es para q al entrar en el fragment, salgan las tres rallicas q dan la opcion a abrir el menu

    private MutableLiveData<String> mText;

    public BaseViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Este es el fragmento");
    }

    public LiveData<String> getText() {
        return mText;
    }
}