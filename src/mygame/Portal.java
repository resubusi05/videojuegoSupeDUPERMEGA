package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Torus;

public class Portal {

    public static final float RADIO_DETECCION = 0.8f;

    private Node      nodo;
    private Vector3f  destino;
    private ColorRGBA color;
    private float     tiempo = 0f;
    private Geometry  aroPulso;

    public Portal(AssetManager assetManager, Node rootNode,
                  Vector3f posicion, Vector3f destino, ColorRGBA color) {

        this.destino = destino;
        this.color   = color;
        this.nodo    = new Node("Portal");

        // ── Aro exterior ──────────────────────────────────────────────
        Geometry aroExt = new Geometry("Aro",
                new Torus(40, 10, 0.07f, 0.75f));
        Material matExt = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        matExt.setColor("Color", color);
        aroExt.setMaterial(matExt);

        // ── Aro interior (más brillante) ───────────────────────────────
        Geometry aroInt = new Geometry("AroInner",
                new Torus(40, 6, 0.03f, 0.52f));
        Material matInt = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        matInt.setColor("Color", color.clone().mult(3f));
        aroInt.setMaterial(matInt);

        // ── Aro de pulso (escala animada) ──────────────────────────────
        aroPulso = new Geometry("AroPulso",
                new Torus(30, 5, 0.02f, 0.65f));
        Material matPulso = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        matPulso.setColor("Color", color.clone().mult(2f));
        aroPulso.setMaterial(matPulso);

        nodo.attachChild(aroExt);
        nodo.attachChild(aroInt);
        nodo.attachChild(aroPulso);
        nodo.setLocalTranslation(posicion);

        rootNode.attachChild(nodo);
    }

    public void actualizar(float tpf) {
        tiempo += tpf;

        // Aro exterior gira en Y
        nodo.getChild("Aro").rotate(0, tpf * 1.2f, 0);

        // Aro interior gira en Z
        nodo.getChild("AroInner").rotate(0, 0, tpf * 2.0f);

        // Aro de pulso: escala que sube y baja como onda senoidal
        float pulso = 1f + 0.15f * FastMath.sin(tiempo * 4f);
        aroPulso.setLocalScale(pulso);
        aroPulso.rotate(0, -tpf * 0.8f, 0);
    }

    public boolean jugadorDentro(Vector3f posJugador) {
        return nodo.getWorldTranslation().distance(posJugador) < RADIO_DETECCION;
    }

    public Vector3f  getDestino()  { return destino; }
    public Vector3f  getPosicion() { return nodo.getWorldTranslation(); }
    public ColorRGBA getColor()    { return color; }
    public Node      getNodo()     { return nodo; }
}