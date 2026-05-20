package mygame;

import com.jme3.light.Light;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

public class ControladorLuz {
    private Light luz;
    private ColorRGBA colorOriginal;
    
    private float temporizador = 0f;
    private float limiteTiempo;
    private boolean encendida = true;

    public ControladorLuz(Light luz) {
        this.luz = luz;
        // Guardamos el color original para saber a qué color volver al encenderla
        this.colorOriginal = luz.getColor().clone(); 
        generarNuevoLimite();
    }

    // Este método lo llamaremos en cada frame
    public void actualizar(float tpf) {
        temporizador += tpf; // Sumamos el tiempo real que ha pasado

        if (temporizador >= limiteTiempo) {
            // ¡Se acabó la paciencia! Cambiamos de estado
            encendida = !encendida;

            if (encendida) {
                luz.setColor(colorOriginal); // Encender
            } else {
                luz.setColor(ColorRGBA.BlackNoAlpha); // Apagar (color negro)
                // Ojo: Si prefieres que no se apague del todo, usa:
                // luz.setColor(colorOriginal.mult(0.2f)); 
            }

            temporizador = 0f; // Reiniciamos el cronómetro
            generarNuevoLimite(); // Calculamos un nuevo tiempo al azar
        }
    }

    private void generarNuevoLimite() {
        
        if(encendida){
            limiteTiempo = 1f + (FastMath.nextRandomFloat() * 2.0f);
        } else {
            limiteTiempo = 0.05f + (FastMath.nextRandomFloat() * 0.15f);
        }
        
    }
}