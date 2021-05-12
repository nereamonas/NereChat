package com.example.nerechat.ui.mapa;

public class MapaElement {
    String usuario;
    Double latitude;
    Double longitude;

    public MapaElement(){}

    public MapaElement(Double latitude, Double longitude,String usuario) {
        this.usuario = usuario;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUsuario() {
        return usuario;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
