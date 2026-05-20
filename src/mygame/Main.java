package mygame;

import java.util.ArrayList;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.Geometry;
import com.jme3.material.Material;
import com.jme3.scene.shape.Quad;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData;

public class Main extends SimpleApplication implements ActionListener, AnalogListener {

    private ArrayList<ControladorLuz> lucesParpadeantes = new ArrayList<>();

    // ── Física ────────────────────────────────────────────────────────────────
    private BulletAppState bulletAppState;
    private CharacterControl playerControl;

    // ── Movimiento ────────────────────────────────────────────────────────────
    private boolean moveForward, moveBackward, moveLeft, moveRight;
    private boolean isRunning = false;

    // ── Cámara / ratón ────────────────────────────────────────────────────────
    private float yaw   = 0f;
    private float pitch = 0f;
    private static final float MOUSE_SPEED = 2.5f;
    private static final float PITCH_MAX   = FastMath.HALF_PI * 0.90f;

    // ── Movimiento del jugador ────────────────────────────────────────────────
    private static final float WALK_SPEED   = 5f;
    private static final float RUN_SPEED    = 9f;   // velocidad al correr
    private static final float STRAFE_SPEED = 3f;
    private static final float CAM_Y_OFFSET = 0.5f;

    // ── Objetos reutilizables ─────────────────────────────────────────────────
    private final Quaternion qYaw   = new Quaternion();
    private final Quaternion qPitch = new Quaternion();
    private final Vector3f   moveDir = new Vector3f();
    private final Vector3f   camDir  = new Vector3f();
    private final Vector3f   camLeft = new Vector3f();

    // ── Luces ─────────────────────────────────────────────────────────────────
    private SpotLight  linterna;
    private PointLight foco;

    // ── Portales ──────────────────────────────────────────────────────────────
    private Portal portalA;
    private Portal portalB;
    private Vector3f posicionO = null;
    private Vector3f posicionP = null;
    private boolean enTeleporte = false;
    private static final ColorRGBA COLOR_PAR_1 = new ColorRGBA(0.2f, 0.6f, 1f, 1f);

    // ── Niña ──────────────────────────────────────────────────────────────────
    private CharacterControl ninaControl;
    private Node nina = null;
    
    private float timerAtascada   = 0f;
    private Vector3f posAnterior  = new Vector3f();
    private float anguloEvasion   = 0f;

    // Estados de la niña
    private enum EstadoNina { OCULTA, SIGUIENDO, DESAPARECIENDO }
    private EstadoNina estadoNina = EstadoNina.OCULTA;
    private AudioNode sonidoNina;


    private float timerAparecer    = 0f;   // tiempo hasta aparecer
    private float timerDesaparecer = 0f;   // tiempo hasta desaparecer mientras sigue
    private float timerReaparecer  = 0f;   // tiempo hasta reaparecer tras desvanecerse

    private static final float VELOCIDAD_NINA     = 2.2f;  // qué tan rápido te sigue
    private static final float DISTANCIA_ALCANCE  = 1.4f;  // distancia para atrapar al jugador
    private static final float DISTANCIA_APARICION = 8f;   // radio de aparición alrededor del jugador

    // ── Game Over ─────────────────────────────────────────────────────────────
    private boolean gameOver = false;
    private Geometry overlayRojo = null;
    private BitmapText textoGameOver = null;

    // ── Posición spawn original ───────────────────────────────────────────────
    private static final Vector3f SPAWN_POS = new Vector3f(0f, 2.8f, 11.4f);

    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        new Main().start();
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    @Override
    public void simpleInitApp() {
        initPhysics();
        initMap();
        initPlayer();
        initLights();
        createGround();
        initNina();
        initHUD();
        registrarInputs();
    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
    }

    private void initMap() {
        Node mapa = (Node) assetManager.loadModel("Scenes/HospitalPsy.obj");
        mapa.setShadowMode(ShadowMode.CastAndReceive);

        Spatial silla = mapa.getChild("Chair0_Back");
        if (silla != null) {
            Material matSilla = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            matSilla.setColor("Diffuse", ColorRGBA.Pink);
            matSilla.setColor("Ambient", ColorRGBA.Pink);
            silla.setMaterial(matSilla);
        }

        CollisionShape mapaShape = CollisionShapeFactory.createMeshShape(mapa);
        RigidBodyControl mapaFisica = new RigidBodyControl(mapaShape, 0);
        mapa.addControl(mapaFisica);
        bulletAppState.getPhysicsSpace().add(mapaFisica);
        rootNode.attachChild(mapa);
    }

    private void initPlayer() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.3f, 1.0f, 1);
        playerControl = new CharacterControl(capsuleShape, 0.05f);
        playerControl.setJumpSpeed(10f);
        playerControl.setFallSpeed(30f);
        playerControl.setGravity(30f);
        playerControl.setPhysicsLocation(SPAWN_POS.clone());
        bulletAppState.getPhysicsSpace().add(playerControl);
    }

    private void initLights() {
        linterna = new SpotLight();
        linterna.setColor(ColorRGBA.White.mult(0.5f));
        linterna.setSpotRange(10f);
        linterna.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
        linterna.setSpotOuterAngle(30f * FastMath.DEG_TO_RAD);
        linterna.setPosition(cam.getLocation());
        linterna.setDirection(cam.getDirection());

        foco = new PointLight();
        foco.setColor(ColorRGBA.White);
        foco.setRadius(3f);
        foco.setPosition(new Vector3f(0f, 2f, 11.4f));

        rootNode.addLight(linterna);
        rootNode.addLight(foco);
        lucesParpadeantes.add(new ControladorLuz(foco));
    }

    private void createGround() {
        Geometry ground = new Geometry("Ground", new Box(200, 0.1f, 200));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setColor("Ambient", ColorRGBA.White);
        ground.setMaterial(mat);
        RigidBodyControl groundControl = new RigidBodyControl(0.0f);
        ground.addControl(groundControl);
        bulletAppState.getPhysicsSpace().add(groundControl);
        rootNode.attachChild(ground);
    }

    private void initNina() {
        nina = (Node) assetManager.loadModel("Models/nina_fragmentada_v3.obj");
        nina.setShadowMode(ShadowMode.CastAndReceive);
        nina.setLocalScale(1f); // ajusta si es necesario

        // Física igual que el jugador
        CapsuleCollisionShape ninaShape = new CapsuleCollisionShape(0.3f, 1.0f, 1);
        ninaControl = new CharacterControl(ninaShape, 0.05f);
        ninaControl.setJumpSpeed(0f);
        ninaControl.setFallSpeed(30f);
        ninaControl.setGravity(30f);
        ninaControl.setPhysicsLocation(new Vector3f(0f, 2.8f, 8f)); // posición inicial oculta
        nina.addControl(ninaControl);
        bulletAppState.getPhysicsSpace().add(ninaControl);

        nina.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(nina);

        timerAparecer = 5f + FastMath.nextRandomFloat() * 10f;
        
        // Sonido posicional (volumen según distancia automático)
        sonidoNina = new AudioNode(assetManager, "Sounds/llorona_mono.ogg", AudioData.DataType.Stream);
        sonidoNina.setLooping(true);
        sonidoNina.setPositional(true);
        sonidoNina.setReverbEnabled(true);
        sonidoNina.setMaxDistance(20f);   // distancia máxima donde se escucha
        sonidoNina.setRefDistance(2f);    // distancia donde suena al máximo volumen
        sonidoNina.setVolume(2f);
        nina.attachChild(sonidoNina);     // va pegado a la niña, se mueve con ella
        sonidoNina.stop();                // silencio al inicio
    }

    /** Crea el overlay rojo (oculto) y el texto de Game Over. */
    private void initHUD() {
        // ── Overlay rojo ──────────────────────────────────────────────────────
        Quad quad = new Quad(settings.getWidth(), settings.getHeight());
        overlayRojo = new Geometry("OverlayRojo", quad);
        Material matRojo = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRojo.setColor("Color", new ColorRGBA(0.8f, 0f, 0f, 0.55f));
        matRojo.getAdditionalRenderState().setBlendMode(
            com.jme3.material.RenderState.BlendMode.Alpha);
        overlayRojo.setMaterial(matRojo);
        overlayRojo.setLocalTranslation(0, 0, 0);
        // Lo ponemos en la GUI para que tape toda la pantalla
        overlayRojo.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(overlayRojo);

        // ── Texto "Presiona R" ─────────────────────────────────────────────────
        BitmapFont fuente = assetManager.loadFont("Interface/Fonts/Default.fnt");
        textoGameOver = new BitmapText(fuente, false);
        textoGameOver.setSize(fuente.getCharSet().getRenderedSize() * 2f);
        textoGameOver.setColor(ColorRGBA.White);
        textoGameOver.setText("¡Te atraparon!\nPresiona R para reiniciar");
        textoGameOver.setLocalTranslation(
            settings.getWidth()  / 2f - 160f,
            settings.getHeight() / 2f + 30f,
            1f);
        textoGameOver.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(textoGameOver);
    }

    // ── Inputs ────────────────────────────────────────────────────────────────

    private void registrarInputs() {
        inputManager.addMapping("Adelante",       new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Atras",          new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Izquierda",      new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Derecha",        new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Saltar",         new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Correr",         new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Reiniciar",      new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("MirarDerecha",   new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("MirarIzquierda", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("MirarArriba",    new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("MirarAbajo",     new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("PortalO",        new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("PortalP",        new KeyTrigger(KeyInput.KEY_P));

        inputManager.addListener(this,
            "Adelante", "Atras", "Izquierda", "Derecha", "Saltar", "Correr", "Reiniciar",
            "MirarDerecha", "MirarIzquierda", "MirarArriba", "MirarAbajo",
            "PortalO", "PortalP");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {

        // ── R solo funciona en game over ──────────────────────────────────────
        if (name.equals("Reiniciar") && isPressed) {
            if (gameOver) reiniciarJuego();
            return;
        }

        // ── Si estamos en game over, bloquear todo lo demás ───────────────────
        if (gameOver) return;

        switch (name) {
            case "Adelante":  moveForward  = isPressed; break;
            case "Atras":     moveBackward = isPressed; break;
            case "Izquierda": moveLeft     = isPressed; break;
            case "Derecha":   moveRight    = isPressed; break;
            case "Correr":    isRunning    = isPressed; break;
            case "Saltar":    if (isPressed) playerControl.jump(); break;

            case "PortalO":
                if (isPressed) {
                    posicionO = playerControl.getPhysicsLocation().clone();
                    if (portalA != null) rootNode.detachChild(portalA.getNodo());
                    Vector3f destA = (posicionP != null) ? posicionP.clone() : posicionO.clone();
                    portalA = new Portal(assetManager, rootNode, posicionO, destA, COLOR_PAR_1);
                    if (portalB != null) portalB = new Portal(assetManager, rootNode,
                            posicionP, posicionO.clone(), COLOR_PAR_1);
                }
                break;

            case "PortalP":
                if (isPressed) {
                    posicionP = playerControl.getPhysicsLocation().clone();
                    if (portalB != null) rootNode.detachChild(portalB.getNodo());
                    Vector3f destB = (posicionO != null) ? posicionO.clone() : posicionP.clone();
                    portalB = new Portal(assetManager, rootNode, posicionP, destB, COLOR_PAR_1);
                    if (portalA != null) portalA = new Portal(assetManager, rootNode,
                            posicionO, posicionP.clone(), COLOR_PAR_1);
                }
                break;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // Bloquear cámara en game over
        if (gameOver) return;

        switch (name) {
            case "MirarDerecha":   yaw   -= value * MOUSE_SPEED; break;
            case "MirarIzquierda": yaw   += value * MOUSE_SPEED; break;
            case "MirarArriba":    pitch -= value * MOUSE_SPEED; break;
            case "MirarAbajo":     pitch += value * MOUSE_SPEED; break;
        }

        pitch = FastMath.clamp(pitch, -PITCH_MAX, PITCH_MAX);
        qYaw.fromAngleAxis(yaw,   Vector3f.UNIT_Y);
        qPitch.fromAngleAxis(pitch, Vector3f.UNIT_X);
        cam.setAxes(qYaw.mult(qPitch));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public void simpleUpdate(float tpf) {

        // ── Si hay game over, no actualizar nada más ──────────────────────────
        if (gameOver) return;

        // ── Velocidad según si corre o camina ────────────────────────────────
        float velActual = isRunning ? RUN_SPEED : WALK_SPEED;

        camDir.set(cam.getDirection()).multLocal(velActual);
        camLeft.set(cam.getLeft()).multLocal(STRAFE_SPEED);
        camDir.y  = 0;
        camLeft.y = 0;

        moveDir.set(0, 0, 0);
        if (moveForward)  moveDir.addLocal(camDir);
        if (moveBackward) moveDir.subtractLocal(camDir);
        if (moveLeft)     moveDir.addLocal(camLeft);
        if (moveRight)    moveDir.subtractLocal(camLeft);

        playerControl.setWalkDirection(moveDir.multLocal(tpf));

        for (ControladorLuz controlador : lucesParpadeantes) {
            controlador.actualizar(tpf);
        }

        // Cámara y linterna
        Vector3f playerPos = playerControl.getPhysicsLocation();
        cam.setLocation(playerPos.add(0, CAM_Y_OFFSET, 0));
        linterna.setPosition(cam.getLocation());
        linterna.setDirection(cam.getDirection());

        // ── Portales ──────────────────────────────────────────────────────────
        if (portalA != null) portalA.actualizar(tpf);
        if (portalB != null) portalB.actualizar(tpf);

        Vector3f posJugador = playerControl.getPhysicsLocation();
        if (!enTeleporte) {
            if (portalA != null && portalB != null) {
                if (portalA.jugadorDentro(posJugador)) {
                    playerControl.setPhysicsLocation(portalB.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                } else if (portalB.jugadorDentro(posJugador)) {
                    playerControl.setPhysicsLocation(portalA.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                }
            }
        } else {
            if (portalA != null && !portalA.jugadorDentro(posJugador)
             && portalB != null && !portalB.jugadorDentro(posJugador)) {
                enTeleporte = false;
            }
        }

        // ── Lógica de la niña ─────────────────────────────────────────────────
        actualizarNina(tpf, posJugador);
    }

    // ── Lógica de la niña ─────────────────────────────────────────────────────

    private void actualizarNina(float tpf, Vector3f posJugador) {

        switch (estadoNina) {

            case OCULTA:
                timerAparecer -= tpf;
                if (timerAparecer <= 0f) {
                    aparecerNina(posJugador);
                }
                break;

            case SIGUIENDO:
                Vector3f posNina   = ninaControl.getPhysicsLocation();
                Vector3f alJugador = posJugador.subtract(posNina);
                alJugador.y = 0;

                // ¿Está atascada? Compara con posición del frame anterior
                timerAtascada += tpf;
                if (timerAtascada >= 0.4f) {
                    float desplazamiento = posNina.distance(posAnterior);
                    if (desplazamiento < 0.05f) {
                        // Lleva 0.4s sin moverse → rotar ángulo de evasión
                        anguloEvasion += 45f * FastMath.DEG_TO_RAD;
                    } else {
                        // Se está moviendo bien → resetear evasión gradualmente
                        anguloEvasion *= 0.5f;
                    }
                    posAnterior.set(posNina);
                    timerAtascada = 0f;
                }

                // Dirección base hacia el jugador + rotación de evasión si está atascada
                Vector3f direccion;
                if (alJugador.lengthSquared() > 0.001f) {
                    alJugador.normalizeLocal();
                    if (Math.abs(anguloEvasion) > 0.01f) {
                        // Rotar la dirección por el ángulo de evasión
                        Quaternion rotEvasion = new Quaternion();
                        rotEvasion.fromAngleAxis(anguloEvasion, Vector3f.UNIT_Y);
                        direccion = rotEvasion.mult(alJugador);
                    } else {
                        direccion = alJugador;
                    }
                } else {
                    direccion = Vector3f.ZERO.clone();
                }

                ninaControl.setWalkDirection(direccion.mult(VELOCIDAD_NINA * tpf));

                // Girar todo el modelo hacia la cámara (no hacia donde camina)
                Vector3f mirarCam = cam.getLocation().subtract(posNina);
                mirarCam.y = 0;
                if (mirarCam.lengthSquared() > 0.001f) {
                    mirarCam.normalizeLocal();
                    float anguloY = FastMath.atan2(mirarCam.x, mirarCam.z);
                    Quaternion rotNina = new Quaternion();
                    rotNina.fromAngleAxis(anguloY, Vector3f.UNIT_Y);
                    nina.setLocalRotation(rotNina);
                }

                // Sincronizar visual con física
                nina.setLocalTranslation(posNina);

                // ¿Te alcanzó?
                if (posNina.distance(posJugador) <= DISTANCIA_ALCANCE) {
                    activarGameOver();
                    return;
                }

                // ¿Desaparece aleatoriamente?
                timerDesaparecer -= tpf;
                if (timerDesaparecer <= 0f) {
                    ocultarNina();
                    estadoNina = EstadoNina.DESAPARECIENDO;
                    timerReaparecer = 3f + FastMath.nextRandomFloat() * 5f;
                    ninaControl.setWalkDirection(Vector3f.ZERO);
                    anguloEvasion = 0f; // resetear evasión al desaparecer
                }
                break;
   

            case DESAPARECIENDO:
                timerReaparecer -= tpf;
                if (timerReaparecer <= 0f) {
                    aparecerNina(posJugador);
                }
                break;
        }
    }

    /** Muestra la niña en una posición aleatoria alrededor del jugador. */
    private void aparecerNina(Vector3f posJugador) {
        float angulo  = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        float offsetX = FastMath.cos(angulo) * DISTANCIA_APARICION;
        float offsetZ = FastMath.sin(angulo) * DISTANCIA_APARICION;
        Vector3f spawn = new Vector3f(
            posJugador.x + offsetX,
            posJugador.y,
            posJugador.z + offsetZ);

        ninaControl.setPhysicsLocation(spawn);
        nina.setCullHint(Spatial.CullHint.Inherit);
        sonidoNina.play(); // empieza el sonido al aparecer

        estadoNina = EstadoNina.SIGUIENDO;
        timerDesaparecer = 4f + FastMath.nextRandomFloat() * 8f;
    }

    /** Oculta la niña sin activar game over. */
    private void ocultarNina() {
        nina.setCullHint(Spatial.CullHint.Always);
        ninaControl.setWalkDirection(Vector3f.ZERO);
        sonidoNina.stop(); // silencio al desaparecer

    }

    // ── Game Over ─────────────────────────────────────────────────────────────

    private void activarGameOver() {
        gameOver = true;
        ocultarNina();
        estadoNina = EstadoNina.OCULTA;

        // Detener al jugador
        playerControl.setWalkDirection(Vector3f.ZERO);
        moveForward = moveBackward = moveLeft = moveRight = false;

        // Mostrar overlay rojo y texto
        overlayRojo.setCullHint(Spatial.CullHint.Inherit);
        textoGameOver.setCullHint(Spatial.CullHint.Inherit);
    }

    private void reiniciarJuego() {
        gameOver = false;

        // Quitar overlay
        overlayRojo.setCullHint(Spatial.CullHint.Always);
        textoGameOver.setCullHint(Spatial.CullHint.Always);

        // Resetear jugador al spawn
        playerControl.setPhysicsLocation(SPAWN_POS.clone());
        playerControl.setWalkDirection(Vector3f.ZERO);

        // Resetear cámara
        yaw   = 0f;
        pitch = 0f;

        // Resetear portales
        if (portalA != null) { rootNode.detachChild(portalA.getNodo()); portalA = null; }
        if (portalB != null) { rootNode.detachChild(portalB.getNodo()); portalB = null; }
        posicionO = null;
        posicionP = null;
        enTeleporte = false;

        // Resetear niña: aparecerá de nuevo entre 5 y 15 segundos
        ocultarNina();
        estadoNina  = EstadoNina.OCULTA;
        timerAparecer = 5f + FastMath.nextRandomFloat() * 10f;
    }

    @Override
    public void simpleRender(RenderManager rm) { /* sin uso */ }
}