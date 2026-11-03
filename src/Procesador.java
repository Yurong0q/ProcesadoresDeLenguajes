import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import java.io.File;
import java.util.List;

import Errores.*;
import tslib.TS_Gestor;
import tslib.TS_Gestor.Tabla;

public class Procesador {

    private static void procesarRuta(String ruta) {
        try {
            Path p = Paths.get(ruta);

            if (!Files.exists(p)) {
                System.out.println("ERROR: No existe el archivo: " + ruta);
                return;
            }

            // Lee el código fuente para los errores
            List<String> codigoFuente = Files.readAllLines(p, StandardCharsets.ISO_8859_1);

            TS_Gestor gestorTs = new TS_Gestor("tabladesimbolos.txt");
            TablaDeSimbolos tabla = new TablaDeSimbolos(gestorTs);
            tabla.iniciar();
            tabla.crearTablaGlobal();

            Analizador_Lexico lexico = new Analizador_Lexico(ruta, gestorTs, codigoFuente);
            LexerConTexto aLex = new LexerConTexto(lexico, "tokens.txt");

            try (FileWriter parsetxt = new FileWriter("parse.txt")) {
                Analizador_Sint_Sem sintactico = new Analizador_Sint_Sem(aLex, codigoFuente, gestorTs);
                String parse = sintactico.iniciar();
                if (parse != null) parsetxt.write(parse);
            } finally {
                // Asegura cierre aunque haya error
                aLex.close();
            }

            // Escribir y destruir tablas
            int res = gestorTs.write(Tabla.GLOBAL);
            if (res != 0) System.out.println("Error al escribir la TS global");

            res = gestorTs.destroy(Tabla.GLOBAL);
            if (res != 0) System.out.println("Error al destruir la TS global");

            res = gestorTs.destroy(Tabla.PALRES);
            if (res != 0) System.out.println("Error al destruir la TS de palabras reservadas");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Err e) {
            System.out.println(e.getMessage());
        }
    }

    private static String elegirArchivoConDialogo() {
        JFrame frame = new JFrame("Selector de archivo:");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);

        String ruta = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ruta = selectedFile.getAbsolutePath();
        }

        frame.dispose();
        return ruta;
    }

    public static void main(String[] args) {
        // 1) Si me pasas la ruta por terminal, uso esa
        if (args.length >= 1) {
            procesarRuta(args[0]);
            return;
        }

        // 2) Si no, abro selector
        String ruta = elegirArchivoConDialogo();
        if (ruta == null) {
            System.out.println("No se seleccionó ningún archivo");
            return;
        }

        procesarRuta(ruta);
    }
}
