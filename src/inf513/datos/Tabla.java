package inf513.datos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resultado tabular (columnas + filas de texto) para mostrar en la respuesta. */
public final class Tabla {

    private final String titulo;
    private final List<String> columnas;
    private final List<String[]> filas = new ArrayList<>();
    private String nota;

    public Tabla(String titulo, String... columnas) {
        this.titulo = titulo;
        this.columnas = Arrays.asList(columnas);
    }

    public Tabla(String titulo, List<String> columnas) {
        this.titulo = titulo;
        this.columnas = columnas;
    }

    public Tabla fila(String... valores) {
        filas.add(valores);
        return this;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<String[]> getFilas() {
        return filas;
    }

    public String getNota() {
        return nota;
    }

    public Tabla nota(String nota) {
        this.nota = nota;
        return this;
    }
}
