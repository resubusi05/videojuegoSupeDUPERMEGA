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
import com.jme3.light.AmbientLight;
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

public class Main extends SimpleApplication implements ActionListener, AnalogListener {

    private ArrayList<ControladorLuz> lucesParpadeantes = new ArrayList<>();
    
    //Llaves
    private Spatial nodoLlave1 = null;
        private Spatial nodoLlave2 = null;

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
    private static final float RUN_SPEED    = 9f;
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

    // ── Portales O/P (dinámicos) ──────────────────────────────────────────────
    private Portal portalA;
    private Portal portalB;
    private Vector3f posicionO = null;
    private Vector3f posicionP = null;
    private boolean enTeleporte = false;
    private static final ColorRGBA COLOR_PAR_1 = new ColorRGBA(0.2f, 0.6f, 1f, 1f);

    // ── Portales fijos ────────────────────────────────────────────────────────
    private Portal portalFijo1 = null;
    private Portal portalFijo2 = null;
    private static final ColorRGBA COLOR_PAR_2 = new ColorRGBA(1f, 0.3f, 0.1f, 1f);  // naranja
    private static final ColorRGBA COLOR_PAR_3 = new ColorRGBA(0.2f, 1f, 0.3f, 1f);  // verde

    // ── Mapa actual ───────────────────────────────────────────────────────────
    private Node nodoMapaActual = null;
    private RigidBodyControl fisicaMapaActual = null;
    private String mapaActual = "";

    // Spawns de cada mapa
    private static final Vector3f SPAWN_HOSPITAL  = new Vector3f(0f, 2.8f, 11.4f);
    private static final Vector3f SPAWN_PASILLO   = new Vector3f(-0.025965627f, 1.4f, -29.518536f);
    private static final Vector3f SPAWN_EXPANDED  = new Vector3f(-0.028759956f, 1.4f,  19.512066f);

    // Posiciones de portales en HospitalPsy
    private static final Vector3f POS_HOSPITAL_A_PASILLO  = new Vector3f(1.2205951f,   0.94324297f, 10.335817f);
    private static final Vector3f POS_HOSPITAL_A_EXPANDED = new Vector3f(-1.3623745f,  0.9470472f,   6.3709793f);

    // Posiciones de portales de regreso
    private static final Vector3f POS_PASILLO_A_HOSPITAL  = new Vector3f(-0.025965627f, 0.9f, -29.518536f);
    private static final Vector3f POS_EXPANDED_A_HOSPITAL = new Vector3f(-0.028759956f, 0.9f,  19.512066f);

    // ── Niña ──────────────────────────────────────────────────────────────────
    private Nina nina;

    // ── Game Over ─────────────────────────────────────────────────────────────
    private boolean gameOver = false;
    private Geometry overlayRojo = null;
    private BitmapText textoGameOver = null;

    // ── Posición spawn original ───────────────────────────────────────────────
    private static final Vector3f SPAWN_POS = SPAWN_HOSPITAL;

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        new Main().start();
    }

    // ── Inicialización ────────────────────────────────────────────────────────
    @Override
    public void simpleInitApp() {
        initPhysics();
        initPlayer();
        initLights();
        createGround();
        initNina();
        cargarMapa("hospital");
        initHUD();
        registrarInputs();
    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
    }

    // ── Carga/descarga dinámica de mapas ──────────────────────────────────────
    private void cargarMapa(String nombreMapa) {
        
        if (nodoLlave1 != null) { rootNode.detachChild(nodoLlave1); nodoLlave1 = null; }
        if (nodoLlave2 != null) { rootNode.detachChild(nodoLlave2); nodoLlave2 = null; }
        
        // ── Descargar mapa anterior ───────────────────────────────────────────
        if (nodoMapaActual != null) {
            if (fisicaMapaActual != null) {
                bulletAppState.getPhysicsSpace().remove(fisicaMapaActual);
                fisicaMapaActual = null;
            }
            rootNode.detachChild(nodoMapaActual);
            nodoMapaActual = null;
        }

        // ── Quitar portales fijos anteriores ──────────────────────────────────
        quitarPortalesFijos();

        // ── Cargar nuevo mapa ─────────────────────────────────────────────────
        String rutaOBJ;
        switch (nombreMapa) {
            case "pasillo":
                rutaOBJ = "Scenes/pasillo_rojo.obj";
                break;
            case "expanded":
                rutaOBJ = "Scenes/hospital_psiquiatrico_expanded.obj";
                break;
            default: // "hospital"
                rutaOBJ = "Scenes/HospitalPsy.obj";
                break;
        }

        nodoMapaActual = (Node) assetManager.loadModel(rutaOBJ);
        nodoMapaActual.setShadowMode(ShadowMode.CastAndReceive);

        CollisionShape shape = CollisionShapeFactory.createMeshShape(nodoMapaActual);
        fisicaMapaActual = new RigidBodyControl(shape, 0);
        nodoMapaActual.addControl(fisicaMapaActual);
        bulletAppState.getPhysicsSpace().add(fisicaMapaActual);
        rootNode.attachChild(nodoMapaActual);

        mapaActual = nombreMapa;

        // ── Crear portales fijos del nuevo mapa ───────────────────────────────
        switch (nombreMapa) {
            case "hospital":
                // Portal naranja → PasilloRojo
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_HOSPITAL_A_PASILLO, POS_PASILLO_A_HOSPITAL, COLOR_PAR_2);
                // Portal verde → Expanded
                portalFijo2 = new Portal(assetManager, rootNode,
                        POS_HOSPITAL_A_EXPANDED, POS_EXPANDED_A_HOSPITAL, COLOR_PAR_3);
                break;
            case "pasillo":
                // Solo portal naranja de regreso al hospital
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_PASILLO_A_HOSPITAL, POS_HOSPITAL_A_PASILLO, COLOR_PAR_2);
                portalFijo2 = null;
                break;
            case "expanded":
                // Solo portal verde de regreso al hospital
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_EXPANDED_A_HOSPITAL, POS_HOSPITAL_A_EXPANDED, COLOR_PAR_3);
                portalFijo2 = null;
                break;
                
        }
        
        // ── Llaves ────────────────────────────────────────────────────────────
        switch (nombreMapa) {
            case "hospital":
                nodoLlave1 = assetManager.loadModel("Models/llave1.obj");
                nodoLlave1.setLocalScale(0.05f);
                nodoLlave1.updateGeometricState(); // ← fuerza cálculo del bounding box
                com.jme3.bounding.BoundingBox bb1 =
                        (com.jme3.bounding.BoundingBox) nodoLlave1.getWorldBound();
                float mitadAltura1 = (bb1 != null) ? bb1.getYExtent() : 0f;
                nodoLlave1.setLocalTranslation(-23.629555f,
                        0.9f - mitadAltura1,   // ← Y ajustada
                        -9.214014f);
                nodoLlave1.rotate(0, 224.23128f * FastMath.DEG_TO_RAD, 0.015f);
                rootNode.attachChild(nodoLlave1);
                break;

            case "expanded":
                nodoLlave2 = assetManager.loadModel("Models/llave2.obj");
                nodoLlave2.updateGeometricState();
                com.jme3.bounding.BoundingBox bb2 =
                        (com.jme3.bounding.BoundingBox) nodoLlave2.getWorldBound();
                float mitadAltura2 = (bb2 != null) ? bb2.getYExtent() : 0f;
                nodoLlave2.setLocalTranslation(24.451488f,
                        0.9f - mitadAltura2,   // ← Y ajustada
                        -18.123974f);
                nodoLlave2.rotate(0, 85.328186f * FastMath.DEG_TO_RAD, 0);
                rootNode.attachChild(nodoLlave2);
                break;
        }
        
    }

    private void quitarPortalesFijos() {
        if (portalFijo1 != null) {
            rootNode.detachChild(portalFijo1.getNodo());
            portalFijo1 = null;
        }
        if (portalFijo2 != null) {
            rootNode.detachChild(portalFijo2.getNodo());
            portalFijo2 = null;
        }
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
        AmbientLight ambiental = new AmbientLight();
        ambiental.setColor(ColorRGBA.White.mult(0.002f));  // 0.3 = tenue, sube hasta 1.0 si quieres más
        rootNode.addLight(ambiental);
        
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
        nina = new Nina(assetManager, bulletAppState, rootNode);
        nina.setOnAlcanzadoListener(() -> activarGameOver());
    }

    private void initHUD() {
        Quad quad = new Quad(settings.getWidth(), settings.getHeight());
        overlayRojo = new Geometry("OverlayRojo", quad);
        Material matRojo = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRojo.setColor("Color", new ColorRGBA(0.8f, 0f, 0f, 0.55f));
        matRojo.getAdditionalRenderState().setBlendMode(
                com.jme3.material.RenderState.BlendMode.Alpha);
        overlayRojo.setMaterial(matRojo);
        overlayRojo.setLocalTranslation(0, 0, 0);
        overlayRojo.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(overlayRojo);

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
        if (name.equals("Reiniciar") && isPressed) {
            if (gameOver) reiniciarJuego();
            return;
        }
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
                    System.out.println("[PortalO] X=" + posicionO.x + "  Y=" + posicionO.y + "  Z=" + posicionO.z);
                    System.out.println("[PortalO] Yaw=" + (yaw * FastMath.RAD_TO_DEG) + "°  Pitch=" + (pitch * FastMath.RAD_TO_DEG) + "°");
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
                    System.out.println("[PortalP] X=" + posicionP.x + "  Y=" + posicionP.y + "  Z=" + posicionP.z);
                    System.out.println("[PortalP] Yaw=" + (yaw * FastMath.RAD_TO_DEG) + "°  Pitch=" + (pitch * FastMath.RAD_TO_DEG) + "°");
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
        if (gameOver) return;

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

        Vector3f playerPos = playerControl.getPhysicsLocation();
        cam.setLocation(playerPos.add(0, CAM_Y_OFFSET, 0));
        linterna.setPosition(cam.getLocation());
        linterna.setDirection(cam.getDirection());

        // ── Portales dinámicos (O/P) ──────────────────────────────────────────
        if (portalA != null) portalA.actualizar(tpf);
        if (portalB != null) portalB.actualizar(tpf);

        // ── Portales fijos ────────────────────────────────────────────────────
        if (portalFijo1 != null) portalFijo1.actualizar(tpf);
        if (portalFijo2 != null) portalFijo2.actualizar(tpf);

        // ── Teletransporte ────────────────────────────────────────────────────
        if (!enTeleporte) {
            // Portales dinámicos O/P
            if (portalA != null && portalB != null) {
                if (portalA.jugadorDentro(playerPos)) {
                    playerControl.setPhysicsLocation(portalB.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                } else if (portalB.jugadorDentro(playerPos)) {
                    playerControl.setPhysicsLocation(portalA.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                }
            }

            // Portales fijos — cambian de mapa
            if (!enTeleporte) {
                if (portalFijo1 != null && portalFijo1.jugadorDentro(playerPos)) {
                    enTeleporte = true;
                    switch (mapaActual) {
                        case "hospital":
                            cargarMapa("pasillo");
                            playerControl.setPhysicsLocation(SPAWN_PASILLO.clone());
                            break;
                        case "pasillo":
                            cargarMapa("hospital");
                            playerControl.setPhysicsLocation(POS_HOSPITAL_A_PASILLO.add(0, 0.5f, 0));
                            break;
                        case "expanded":
                            cargarMapa("hospital");
                            playerControl.setPhysicsLocation(POS_HOSPITAL_A_EXPANDED.add(0, 0.5f, 0));
                            break;
                    }
                } else if (portalFijo2 != null && portalFijo2.jugadorDentro(playerPos)) {
                    // portalFijo2 solo existe en "hospital" → va a expanded
                    enTeleporte = true;
                    cargarMapa("expanded");
                    playerControl.setPhysicsLocation(SPAWN_EXPANDED.clone());
                }
            }
        } else {
            // Salir del estado enTeleporte cuando el jugador ya no esté en ningún portal
            boolean fueraOP =
                (portalA == null || !portalA.jugadorDentro(playerPos)) &&
                (portalB == null || !portalB.jugadorDentro(playerPos));
            boolean fueraFijos =
                (portalFijo1 == null || !portalFijo1.jugadorDentro(playerPos)) &&
                (portalFijo2 == null || !portalFijo2.jugadorDentro(playerPos));
            if (fueraOP && fueraFijos) enTeleporte = false;
        }

        // ── Niña ──────────────────────────────────────────────────────────────
        nina.actualizar(tpf, playerPos, cam);
    }

    // ── Game Over ─────────────────────────────────────────────────────────────
    private void activarGameOver() {
        gameOver = true;
        nina.ocultar();
        playerControl.setWalkDirection(Vector3f.ZERO);
        moveForward = moveBackward = moveLeft = moveRight = false;
        overlayRojo.setCullHint(Spatial.CullHint.Inherit);
        textoGameOver.setCullHint(Spatial.CullHint.Inherit);
    }

    private void reiniciarJuego() {
        gameOver = false;
        overlayRojo.setCullHint(Spatial.CullHint.Always);
        textoGameOver.setCullHint(Spatial.CullHint.Always);

        // Volver al hospital
        cargarMapa("hospital");
        playerControl.setPhysicsLocation(SPAWN_POS.clone());
        playerControl.setWalkDirection(Vector3f.ZERO);
        yaw   = 0f;
        pitch = 0f;

        // Limpiar portales dinámicos
        if (portalA != null) { rootNode.detachChild(portalA.getNodo()); portalA = null; }
        if (portalB != null) { rootNode.detachChild(portalB.getNodo()); portalB = null; }
        posicionO   = null;
        posicionP   = null;
        enTeleporte = false;

        nina.resetear();
    }

    @Override
    public void simpleRender(RenderManager rm) { /* sin uso */ }
}