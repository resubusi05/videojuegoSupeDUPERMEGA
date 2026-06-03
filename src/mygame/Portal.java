package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box; // Cambiado de Torus a Box

public class Portal {

    public static final float RADIO_DETECCION = 0.8f;

    private Node      nodo;
    private Vector3f  destino;
    private ColorRGBA color;
    private float     tiempo = 0f;
    private Geometry  centroPulso; // Reemplaza a aroPulso

    public Portal(AssetManager assetManager, Node rootNode,
                  Vector3f posicion, Vector3f destino, ColorRGBA color) {

        this.destino = destino;
        this.color   = color;
        this.nodo    = new Node("Portal");

        // ── Palo Vertical ─────────────────────────────────────────────────
        // Extensión: x=0.06 (ancho), y=0.7 (alto total 1.4), z=0.06 (grosor)
        Geometry paloVert = new Geometry("PaloVertical", new Box(0.06f, 0.7f, 0.06f));
        Material matVert = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matVert.setColor("Color", color);
        paloVert.setMaterial(matVert);

        // ── Palo Horizontal (Posicionado abajo para ser invertida) ────────
        // Extensión: x=0.45 (ancho total 0.9), y=0.06, z=0.06
        Geometry paloHoriz = new Geometry("PaloHorizontal", new Box(0.45f, 0.06f, 0.06f));
        Material matHoriz = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matHoriz.setColor("Color", color.clone().mult(1.5f)); // Un poco más vivo
        paloHoriz.setMaterial(matHoriz);

        // Desplazamos el palo horizontal hacia abajo en el eje Y
        paloHoriz.setLocalTranslation(0f, -0.3f, 0f);

        // ── Centro de energía / Pulso (En la intersección) ────────────────
        centroPulso = new Geometry("CentroPulso", new Box(0.09f, 0.09f, 0.09f));
        Material matPulso = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matPulso.setColor("Color", color.clone().mult(3f)); // Brillo intenso
        centroPulso.setMaterial(matPulso);

        // Se coloca exactamente en la misma intersección
        centroPulso.setLocalTranslation(0f, -0.3f, 0f);

        // Adjuntar todo al nodo principal del portal
        nodo.attachChild(paloVert);
        nodo.attachChild(paloHoriz);
        nodo.attachChild(centroPulso);
        nodo.setLocalTranslation(posicion);

        rootNode.attachChild(nodo);
    }

    public void actualizar(float tpf) {
        tiempo += tpf;

        // Rotación de la cruz completa en el eje Y (giro tridimensional)
        nodo.rotate(0, tpf * 1.5f, 0);

        // El núcleo central rota en sentido opuesto para dar dinamismo
        centroPulso.rotate(0, -tpf * 2.5f, 0);

        // Efecto de onda/pulso de tamaño en la intersección
        float pulso = 1f + 0.25f * FastMath.sin(tiempo * 5f);
        centroPulso.setLocalScale(pulso);
    }

    public boolean jugadorDentro(Vector3f posJugador) {
        return nodo.getWorldTranslation().distance(posJugador) < RADIO_DETECCION;
    }

    public Vector3f  getDestino()  { return destino; }
    public Vector3f  getPosicion() { return nodo.getWorldTranslation(); }
    public ColorRGBA getColor()    { return color; }
    public Node      getNodo()     { return nodo; }
}