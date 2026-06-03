package mygame;



import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Nina {

    // ── Estado ────────────────────────────────────────────────────────────────
    public enum Estado { OCULTA, SIGUIENDO, DESAPARECIENDO }
    private Estado estado = Estado.OCULTA;

    // ── Nodo y física ─────────────────────────────────────────────────────────
    private Node nodo;
    private CharacterControl control;
    private AudioNode sonido;

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final float VELOCIDAD        = 2.2f;
    private static final float DISTANCIA_ALCANCE = 1.0f;
    private static final float DISTANCIA_APARICION = 10f;

    // ── Timers y anti-atasco ──────────────────────────────────────────────────
    private float timerAparecer    = 0f;
    private float timerDesaparecer = 0f;
    private float timerReaparecer  = 0f;
    private float timerAtascada    = 0f;
    private float anguloEvasion    = 0f;
    private Vector3f posAnterior   = new Vector3f();

    // ── Callback para notificar game over ─────────────────────────────────────
    public interface OnAlcanzadoListener {
        void onAlcanzado();
    }
    private OnAlcanzadoListener listener;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Nina(AssetManager assetManager, BulletAppState bulletAppState, Node rootNode) {
        nodo = (Node) assetManager.loadModel("Models/nina_fragmentada_v3.obj");
        nodo.setLocalScale(1f);

        CapsuleCollisionShape shape = new CapsuleCollisionShape(0.3f, 1.0f, 1);
        control = new CharacterControl(shape, 0.05f);
        control.setJumpSpeed(0f);
        control.setFallSpeed(30f);
        control.setGravity(30f);
        control.setPhysicsLocation(new Vector3f(0f, 2.8f, 8f));

        nodo.addControl(control);
        bulletAppState.getPhysicsSpace().add(control);
        nodo.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(nodo);

        // Sonido
        sonido = new AudioNode(assetManager, "Sounds/llorona_mono.ogg", AudioData.DataType.Stream);
        sonido.setLooping(true);
        sonido.setPositional(true);
        sonido.setReverbEnabled(true);
        sonido.setMaxDistance(20f);
        sonido.setRefDistance(2f);
        sonido.setVolume(2f);
        nodo.attachChild(sonido);
        sonido.stop();

        timerAparecer = 5f + FastMath.nextRandomFloat() * 10f;
    }

    public void setOnAlcanzadoListener(OnAlcanzadoListener listener) {
        this.listener = listener;
    }

    // ── Update principal ──────────────────────────────────────────────────────
    public void actualizar(float tpf, Vector3f posJugador, Camera cam) {
        switch (estado) {
            case OCULTA:
                timerAparecer -= tpf;
                if (timerAparecer <= 0f) aparecer(posJugador);
                break;

            case SIGUIENDO:
                moverHaciaJugador(tpf, posJugador, cam);
                break;

            case DESAPARECIENDO:
                timerReaparecer -= tpf;
                if (timerReaparecer <= 0f) aparecer(posJugador);
                break;
        }
    }

    private void moverHaciaJugador(float tpf, Vector3f posJugador, Camera cam) {
        Vector3f posNina   = control.getPhysicsLocation();
        Vector3f alJugador = posJugador.subtract(posNina);
        alJugador.y = 0;

        // Anti-atasco
        timerAtascada += tpf;
        if (timerAtascada >= 0.4f) {
            float desplazamiento = posNina.distance(posAnterior);
            anguloEvasion = (desplazamiento < 0.05f)
                    ? anguloEvasion + 45f * FastMath.DEG_TO_RAD
                    : anguloEvasion * 0.5f;
            posAnterior.set(posNina);
            timerAtascada = 0f;
        }

        // Dirección con evasión
        Vector3f direccion = Vector3f.ZERO.clone();
        if (alJugador.lengthSquared() > 0.001f) {
            alJugador.normalizeLocal();
            if (Math.abs(anguloEvasion) > 0.01f) {
                Quaternion rot = new Quaternion();
                rot.fromAngleAxis(anguloEvasion, Vector3f.UNIT_Y);
                direccion = rot.mult(alJugador);
            } else {
                direccion = alJugador;
            }
        }
        control.setWalkDirection(direccion.mult(VELOCIDAD * tpf));

        // Girar hacia la cámara
        Vector3f mirarCam = cam.getLocation().subtract(posNina);
        mirarCam.y = 0;
        if (mirarCam.lengthSquared() > 0.001f) {
            mirarCam.normalizeLocal();
            float anguloY = FastMath.atan2(mirarCam.x, mirarCam.z);
            Quaternion rotNina = new Quaternion();
            rotNina.fromAngleAxis(anguloY, Vector3f.UNIT_Y);
            nodo.setLocalRotation(rotNina);
        }

        nodo.setLocalTranslation(posNina);

        // ¿Alcanzó al jugador?
        if (posNina.distance(posJugador) <= DISTANCIA_ALCANCE) {
            ocultar();
            if (listener != null) listener.onAlcanzado();
            return;
        }

        // ¿Desaparece?
        timerDesaparecer -= tpf;
        if (timerDesaparecer <= 0f) {
            ocultar();
            estado = Estado.DESAPARECIENDO;
            timerReaparecer = 3f + FastMath.nextRandomFloat() * 5f;
            anguloEvasion = 0f;
        }
    }

    private void aparecer(Vector3f posJugador) {
        float angulo  = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        float offsetX = FastMath.cos(angulo) * DISTANCIA_APARICION;
        float offsetZ = FastMath.sin(angulo) * DISTANCIA_APARICION;
        Vector3f spawn = new Vector3f(
                posJugador.x + offsetX,
                posJugador.y,
                posJugador.z + offsetZ);
        control.setPhysicsLocation(spawn);
        nodo.setCullHint(Spatial.CullHint.Inherit);
        sonido.play();
        estado = Estado.SIGUIENDO;
        timerDesaparecer = 4f + FastMath.nextRandomFloat() * 8f;
    }

    public void ocultar() {
        nodo.setCullHint(Spatial.CullHint.Always);
        control.setWalkDirection(Vector3f.ZERO);
        sonido.stop();
        estado = Estado.OCULTA;
    }

    public void resetear() {
        ocultar();
        timerAparecer = 5f + FastMath.nextRandomFloat() * 10f;
        anguloEvasion = 0f;
    }
}