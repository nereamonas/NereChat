package com.example.nerechat.helpClass;

public class Chat {

    private String mensaje;
    private String usuario;

    public Chat(){}

    public Chat(String mensaje, String usuario) {
        this.mensaje = mensaje;
        this.usuario = usuario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
