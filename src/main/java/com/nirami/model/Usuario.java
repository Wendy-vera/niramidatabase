package com.nirami.model;

import java.sql.Timestamp;

public class Usuario {
    private int idUsuario;
    private String nombre;
    private String correo;
    private String telefono;
    private String contrasenaHash;
    private String tipoUsuario;
    private String fotoPerfil;
    private String estado;
    private int intentosFallidos;
    private Timestamp bloqueadoHasta;
    private Timestamp fechaRegistro;
    private Timestamp ultimaSesion;

    // Constructores
    public Usuario() {}

    public Usuario(int idUsuario, String nombre, String correo, String telefono, String contrasenaHash, String tipoUsuario, String fotoPerfil, String estado, int intentosFallidos, Timestamp bloqueadoHasta, Timestamp fechaRegistro, Timestamp ultimaSesion) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.contrasenaHash = contrasenaHash;
        this.tipoUsuario = tipoUsuario;
        this.fotoPerfil = fotoPerfil;
        this.estado = estado;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.fechaRegistro = fechaRegistro;
        this.ultimaSesion = ultimaSesion;
    }

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public Timestamp getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Timestamp bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Timestamp getUltimaSesion() { return ultimaSesion; }
    public void setUltimaSesion(Timestamp ultimaSesion) { this.ultimaSesion = ultimaSesion; }
}
