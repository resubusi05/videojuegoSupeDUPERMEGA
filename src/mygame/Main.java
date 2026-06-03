package mygame;

import java.util.ArrayList;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
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
 
    
    private BitmapText tituloJuego;
    
    // ── Llaves ─────────────────────────────────────────────────────────────────
    private Spatial nodoLlave1 = null;
    private Spatial nodoLlave2 = null;
    private boolean llave1Recogida = false;
    private boolean llave2Recogida = false;
    private int llavesRecogidas = 0;
    private BitmapText textoLlaves = null;
    private BitmapText textoPromptLlave = null;

    // ── Caja Fuerte y Victoria ─────────────────────────────────────────────────
    private Spatial nodoCajaFuerte = null;
    private boolean juegoGanado = false;
    private Geometry overlayVerde = null;
    private BitmapText textoVictoria = null;

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

    // ── CONFIGURACIÓN DE DIFICULTAD ──────────────────────────────────
    private boolean enMenuDificultad = true;
    private Geometry overlayMenu = null;
    private BitmapText textoMenu = null;

    // Variables dinámicas de velocidad
    private float walkSpeed   = 5f;  
    private float runSpeed    = 9f;  
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
    private final ArrayList<com.jme3.light.Light> lucesMapa = new ArrayList<>();

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
    private static final ColorRGBA COLOR_PAR_2 = new ColorRGBA(1f, 0.3f, 0.1f, 1f);   
    private static final ColorRGBA COLOR_PAR_3 = new ColorRGBA(0.2f, 1f, 0.3f, 1f);   
    private static final ColorRGBA COLOR_PAR_4 = new ColorRGBA(0.7f, 0.2f, 1f, 1f);   

    // ── Mapa actual ───────────────────────────────────────────────────────────
    private Node nodoMapaActual = null;
    private RigidBodyControl fisicaMapaActual = null;
    private String mapaActual = "";

    // ── Spawns de cada mapa ───────────────────────────────────────────────────
    private static final Vector3f SPAWN_HOSPITAL  = new Vector3f(0f, 2.8f, 11.4f);
    private static final Vector3f SPAWN_PASILLO   = new Vector3f(-0.025965627f, 1.4f, -29.518536f);
    private static final Vector3f SPAWN_EXPANDED  = new Vector3f(-0.028759956f, 1.4f,  19.512066f);
    private static final Vector3f SPAWN_GRANDE    = new Vector3f(0f, 1.4f, 0f);

    // ── Posiciones portales HospitalPsy ──────────────────────────────────────
    private static final Vector3f POS_HOSPITAL_A_PASILLO  = new Vector3f(1.2205951f,  0.94324297f, 10.335817f);
    private static final Vector3f POS_HOSPITAL_A_EXPANDED = new Vector3f(-1.3623745f, 0.9470472f,   6.3709793f);

    // ── Posiciones portales Pasillo ───────────────────────────────────────────
    private static final Vector3f POS_PASILLO_A_HOSPITAL  = new Vector3f(-0.025965627f, 0.9f, -29.518536f);
    private static final Vector3f POS_PASILLO_A_EXPANDED  = new Vector3f(0.023550153f,  0.9f,  29.53489f);

    // ── Posiciones portales Expanded ─────────────────────────────────────────
    private static final Vector3f POS_EXPANDED_A_HOSPITAL = new Vector3f(-0.028759956f, 0.9f,  19.512066f);
    private static final Vector3f POS_EXPANDED_A_GRANDE   = new Vector3f(-11.060001f,   0.9f, -16.614017f);

    // ── Posiciones portales Grande ────────────────────────────────────────────
    private static final Vector3f POS_GRANDE_A_EXPANDED   = new Vector3f(0.0f,  0.9f,  0.0f);

    // ── Niña ──────────────────────────────────────────────────────────────────
    private Nina nina;

    // ── Game Over y Nuevo Audio de Muerte ──────────────────────────────────────
    private boolean gameOver = false;
    private Geometry overlayRojo = null;
    private BitmapText textoGameOver = null;
    private AudioNode audioMuerte; // Varibale para el audio de muerte
    
    // Audio ambiente
    private AudioNode audioSuspenso;
    private AudioNode audioLatidos;
    private AudioNode audioRisa;

    // Temporizador para risa aleatoria
    private float tiempoRisa = 0f;
    private float siguienteRisa = 15f;

    // ── Spawn original ────────────────────────────────────────────────────────
    private static final Vector3f SPAWN_POS = SPAWN_HOSPITAL;

    public static void main(String[] args) {
        new Main().start();
    }

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

        // Inicialización del archivo de sonido de muerte (2D para máximo impacto)
        audioMuerte = new AudioNode(assetManager, "Sounds/muerte.ogg", AudioData.DataType.Buffer);
        audioMuerte.setLooping(false);
        audioMuerte.setPositional(false); 
        audioMuerte.setVolume(1.8f);
        rootNode.attachChild(audioMuerte);
        
        // Música ambiente en loop
        audioSuspenso = new AudioNode(
                assetManager,
                "Sounds/Sonido_background_suspenso_PasilloRojo.ogg",
                AudioData.DataType.Buffer);

        audioSuspenso.setLooping(true);
        audioSuspenso.setPositional(false);
        audioSuspenso.setVolume(0.15f); // bajito
        rootNode.attachChild(audioSuspenso);
        audioSuspenso.play();

        // Latidos
        audioLatidos = new AudioNode(
                assetManager,
                "Sounds/Latidos_Corazon.ogg",
                AudioData.DataType.Buffer);

        audioLatidos.setLooping(true);
        audioLatidos.setPositional(false);
        audioLatidos.setVolume(0.8f);
        rootNode.attachChild(audioLatidos);

        // Risa lejana
        audioRisa = new AudioNode(
                assetManager,
                "Sounds/Risa_Loco_Eco.ogg",
                AudioData.DataType.Buffer);

        audioRisa.setLooping(false);
        audioRisa.setPositional(false);
        audioRisa.setVolume(0.25f); // se escucha lejos
        rootNode.attachChild(audioRisa);

        // Tiempo inicial aleatorio (mínimo 10 segundos)
        siguienteRisa = 10f + FastMath.nextRandomFloat() * 15f;
    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
    }

    private void cargarMapa(String nombreMapa) {

        if (nodoLlave1 != null) { rootNode.detachChild(nodoLlave1); nodoLlave1 = null; }
        if (nodoLlave2 != null) { rootNode.detachChild(nodoLlave2); nodoLlave2 = null; }
        if (nodoCajaFuerte != null) { rootNode.detachChild(nodoCajaFuerte); nodoCajaFuerte = null; }

        if (nodoMapaActual != null) {
            if (fisicaMapaActual != null) {
                bulletAppState.getPhysicsSpace().remove(fisicaMapaActual);
                nodoMapaActual.removeControl(fisicaMapaActual);
                fisicaMapaActual = null;
            }
            bulletAppState.getPhysicsSpace().removeAll(nodoMapaActual);
            rootNode.detachChild(nodoMapaActual);
            nodoMapaActual = null;
        }

        quitarPortalesFijos();

        String rutaOBJ;
        switch (nombreMapa) {
            case "pasillo":
                rutaOBJ = "Scenes/pasillo_rojo.obj";
                break;
            case "expanded":
                rutaOBJ = "Scenes/hospital_psiquiatrico_expanded.obj";
                break;
            case "grande":
                rutaOBJ = "Scenes/MapaGrandeHospitalPsi.obj";
                break;
            default:
                rutaOBJ = "Scenes/HospitalPsy.obj";
                break;
        }

        nodoMapaActual = (Node) assetManager.loadModel(rutaOBJ).clone();
        nodoMapaActual.setShadowMode(ShadowMode.CastAndReceive);

        CollisionShape shape = CollisionShapeFactory.createMeshShape(nodoMapaActual);
        fisicaMapaActual = new RigidBodyControl(shape, 0);
        nodoMapaActual.addControl(fisicaMapaActual);
        bulletAppState.getPhysicsSpace().add(fisicaMapaActual);
        rootNode.attachChild(nodoMapaActual);

        mapaActual = nombreMapa;

        switch (nombreMapa) {
            case "hospital":
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_HOSPITAL_A_PASILLO, POS_PASILLO_A_HOSPITAL, COLOR_PAR_2);
                portalFijo2 = null;
                break;

            case "pasillo":
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_PASILLO_A_HOSPITAL, POS_HOSPITAL_A_PASILLO, COLOR_PAR_2);
                portalFijo2 = new Portal(assetManager, rootNode,
                        POS_PASILLO_A_EXPANDED, POS_EXPANDED_A_HOSPITAL, COLOR_PAR_3);
                break;

            case "expanded":
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_EXPANDED_A_HOSPITAL, POS_PASILLO_A_EXPANDED, COLOR_PAR_3);
                portalFijo2 = new Portal(assetManager, rootNode,
                        POS_EXPANDED_A_GRANDE, POS_GRANDE_A_EXPANDED, COLOR_PAR_4);
                break;

            case "grande":
                portalFijo1 = new Portal(assetManager, rootNode,
                        POS_GRANDE_A_EXPANDED, POS_EXPANDED_A_GRANDE, COLOR_PAR_4);
                portalFijo2 = null;
                break;
        }

        // ── Llaves ────────────────────────────────────────────────────────────
        if (nombreMapa.equals("hospital") && !llave1Recogida) {
            nodoLlave1 = assetManager.loadModel("Models/llave1.obj");
            nodoLlave1.setLocalScale(0.03f);
            nodoLlave1.updateGeometricState();
            com.jme3.bounding.BoundingBox bb1 =
                    (com.jme3.bounding.BoundingBox) nodoLlave1.getWorldBound();
            float mitadAltura1 = (bb1 != null) ? bb1.getYExtent() : 0f;
            nodoLlave1.setLocalTranslation(-23.629555f, 0.6f - mitadAltura1, -9.214014f);
            nodoLlave1.rotate(0, 224.23128f * FastMath.DEG_TO_RAD, 0.015f);
            rootNode.attachChild(nodoLlave1);
        }

        if (nombreMapa.equals("expanded") && !llave2Recogida) {
            nodoLlave2 = assetManager.loadModel("Models/llave2.obj");
            nodoLlave2.updateGeometricState();
            nodoLlave2.setLocalScale(0.05f);
            com.jme3.bounding.BoundingBox bb2 =
                    (com.jme3.bounding.BoundingBox) nodoLlave2.getWorldBound();
            float mitadAltura2 = (bb2 != null) ? bb2.getYExtent() : 0f;
            nodoLlave2.setLocalTranslation(24.451488f, 0.6f - mitadAltura2, -18.123974f);
            nodoLlave2.rotate(0, 85.328186f * FastMath.DEG_TO_RAD, 0);
            rootNode.attachChild(nodoLlave2);
        }

        // ── Carga de la Caja Fuerte ───────────────────────────────────────────
        if (nombreMapa.equals("grande")) {
            nodoCajaFuerte = assetManager.loadModel("Models/caja_fuerte.obj");
            nodoCajaFuerte.setLocalTranslation(0.1267921f, 1.0268862f, 18.163763f);

            float yawRad   = 265.21677f * FastMath.DEG_TO_RAD;
            float pitchRad = 15.247167f * FastMath.DEG_TO_RAD;
            Quaternion rotCaja = new Quaternion();
            rotCaja.fromAngles(pitchRad, yawRad, 0f);
            nodoCajaFuerte.setLocalRotation(rotCaja);

            rootNode.attachChild(nodoCajaFuerte);
        }

        configurarLucesMapa(nombreMapa);
    }

    private void configurarLucesMapa(String nombreMapa) {
        // Remover luces del mapa anterior
        for (com.jme3.light.Light l : lucesMapa) {
            rootNode.removeLight(l);
        }
        lucesMapa.clear();

        if (nombreMapa.equals("expanded")) {
            ColorRGBA colorAmarillo = new ColorRGBA(1.0f, 0.84f, 0.3f, 1.0f).mult(1.5f);
            ColorRGBA colorSalaIzq = new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f).mult(2.0f);

            // 1. Luces del pasillo principal
            float[] zPasilloPrincipal = { -18.67f, -15.68f, -11.19f, -6.70f, -2.20f, 2.29f, 6.78f, 11.27f, 15.77f };
            for (float z : zPasilloPrincipal) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(6.0f);
                pl.setPosition(new Vector3f(0f, 2.2f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 2. Luces del pasillo horizontal
            float[] xPasilloHorizontal = { 4.49f, 7.49f, 10.48f, 13.48f };
            for (float x : xPasilloHorizontal) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(5.0f);
                pl.setPosition(new Vector3f(x, 2.2f, -18.67f));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 3. Luz sala izquierda
            PointLight plSalaIzq = new PointLight();
            plSalaIzq.setColor(colorSalaIzq);
            plSalaIzq.setRadius(6.0f);
            plSalaIzq.setPosition(new Vector3f(-7.86f, 2.2f, -16.62f));
            rootNode.addLight(plSalaIzq);
            lucesMapa.add(plSalaIzq);

            // 4. Grupo de luces sala superior
            Vector3f[] lucesSalaSup = {
                new Vector3f(-1.12f, 2.2f, -25.04f),
                new Vector3f(0.37f, 2.2f, -24.67f),
                new Vector3f(1.87f, 2.2f, -24.29f)
            };
            for (Vector3f pos : lucesSalaSup) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(5.0f);
                pl.setPosition(pos);
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }
        } else if (nombreMapa.equals("grande")) {
            ColorRGBA colorAmarillo = new ColorRGBA(1.0f, 0.84f, 0.3f, 1.0f).mult(1.5f);
            ColorRGBA colorOficina = new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f).mult(2.0f);

            // 1. Corredor Izquierdo (X = 14.52f)
            float[] zCorredor = { -55.91f, -50.30f, -41.32f, -32.35f, -23.37f, -14.39f, -5.41f, 3.57f, 12.55f };
            for (float z : zCorredor) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(7.0f);
                pl.setPosition(new Vector3f(14.52f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 2. Corredor Derecho (X = 35.48f)
            for (float z : zCorredor) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(7.0f);
                pl.setPosition(new Vector3f(35.48f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 3. Eje Central (X = 25.0f)
            float[] zEjeCentral = { -50.30f, -41.32f, -32.35f, -23.37f, -14.39f, -5.41f, 3.57f, 12.55f };
            for (float z : zEjeCentral) {
                PointLight pl = new PointLight();
                pl.setColor(colorAmarillo);
                pl.setRadius(7.0f);
                pl.setPosition(new Vector3f(25.0f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 4. Oficinas Izquierda (X = -1.95f)
            float[] zOficinas = { -45.81f, -32.35f, -14.39f, 8.06f };
            for (float z : zOficinas) {
                PointLight pl = new PointLight();
                pl.setColor(colorOficina);
                pl.setRadius(10.0f);
                pl.setPosition(new Vector3f(-1.95f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }

            // 5. Oficinas Derecha (X = 51.95f)
            for (float z : zOficinas) {
                PointLight pl = new PointLight();
                pl.setColor(colorOficina);
                pl.setRadius(10.0f);
                pl.setPosition(new Vector3f(51.95f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }
        } else if (nombreMapa.equals("pasillo")) {
            ColorRGBA colorRojo = new ColorRGBA(1.0f, 0.0f, 0.0f, 1.0f).mult(3.0f);

            // Reducimos a 3 luces con mayor radio (22f) para no exceder el límite de luces
            // simultáneas por geometría de JME (máx 4) y evitar parpadeos/apagados.
            float[] zPasillo = { -20.0f, 0.0f, 20.0f };
            for (float z : zPasillo) {
                PointLight pl = new PointLight();
                pl.setColor(colorRojo);
                pl.setRadius(22.0f);
                pl.setPosition(new Vector3f(0f, 2.4f, z));
                rootNode.addLight(pl);
                lucesMapa.add(pl);
            }
        }
    }

    private void intentarRecogerLlave() {
        Vector3f posJugador = playerControl.getPhysicsLocation();

        if (nodoLlave1 != null && !llave1Recogida) {
            if (posJugador.distance(nodoLlave1.getWorldTranslation()) <= 2f) {
                rootNode.detachChild(nodoLlave1);
                nodoLlave1 = null;
                llave1Recogida = true;
                llavesRecogidas++;
                textoLlaves.setText("Llaves: " + llavesRecogidas + "/2");
                textoPromptLlave.setCullHint(Spatial.CullHint.Always);
                return;
            }
        }

        if (nodoLlave2 != null && !llave2Recogida) {
            if (posJugador.distance(nodoLlave2.getWorldTranslation()) <= 2f) {
                rootNode.detachChild(nodoLlave2);
                nodoLlave2 = null;
                llave2Recogida = true;
                llavesRecogidas++;
                textoLlaves.setText("Llaves: " + llavesRecogidas + "/2");
                textoPromptLlave.setCullHint(Spatial.CullHint.Always);
                return;
            }
        }
    }

    private void intentarAbrirCajaFuerte() {
        if (nodoCajaFuerte != null && mapaActual.equals("grande")) {
            Vector3f posJugador = playerControl.getPhysicsLocation();
            if (posJugador.distance(nodoCajaFuerte.getWorldTranslation()) <= 2.5f) {
                if (llavesRecogidas >= 2) {
                    activarVictoria();
                }
            }
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
        ambiental.setColor(ColorRGBA.White.mult(0.002f));
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
        BitmapFont fuente = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        tituloJuego = new BitmapText(fuente, false);
        tituloJuego.setSize(fuente.getCharSet().getRenderedSize() * 6f);        tituloJuego.setColor(ColorRGBA.Red);
        tituloJuego.setText("AND!");
        tituloJuego.setLocalTranslation(
                settings.getWidth() / 2f - 90f,
                settings.getHeight() / 2f + 220f,
                1f);

        guiNode.attachChild(tituloJuego);

        // ── HUD MENU DIFICULTAD ─────────────────────────────────────
        overlayMenu = new Geometry("OverlayMenu", quad);
        Material matMenu = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matMenu.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.05f, 0.95f)); 
        overlayMenu.setMaterial(matMenu);
        overlayMenu.setLocalTranslation(0, 0, 0);
        guiNode.attachChild(overlayMenu);

        textoMenu = new BitmapText(fuente, false);
        textoMenu.setSize(fuente.getCharSet().getRenderedSize() * 1.5f);
        textoMenu.setColor(ColorRGBA.White);
        textoMenu.setText("SELECCIONA LA DIFICULTAD:\n\n" +
                "[1] FACIL   -> Caminas mas rapido que la nina.\n" +
                "[2] MEDIO   -> Velocidad estandar equilibrada.\n" +
                "[3] DIFICIL -> La nina es mas rapida que tu corriendo.");
        textoMenu.setLocalTranslation(
                settings.getWidth()  / 2f - 240f,
                settings.getHeight() / 2f + 80f,
                1f);
        guiNode.attachChild(textoMenu);

        // HUD Game Over
        overlayRojo = new Geometry("OverlayRojo", quad);
        Material matRojo = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRojo.setColor("Color", new ColorRGBA(0.8f, 0f, 0f, 0.55f));
        matRojo.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        overlayRojo.setMaterial(matRojo);
        overlayRojo.setLocalTranslation(0, 0, 0);
        overlayRojo.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(overlayRojo);

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

        // HUD Pantalla Victoria
        overlayVerde = new Geometry("OverlayVerde", quad);
        Material matVerde = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matVerde.setColor("Color", new ColorRGBA(0.0f, 0.35f, 0.15f, 0.7f));
        matVerde.getAdditionalRenderState().setBlendMode(com.jme3.material.RenderState.BlendMode.Alpha);
        overlayVerde.setMaterial(matVerde);
        overlayVerde.setLocalTranslation(0, 0, 0);
        overlayVerde.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(overlayVerde);

        textoVictoria = new BitmapText(fuente, false);
        textoVictoria.setSize(fuente.getCharSet().getRenderedSize() * 1.8f);
        textoVictoria.setColor(ColorRGBA.White);
        textoVictoria.setText("¡Liberaste el alma de La Llorona!\n¡Has ganado el juego!\n\nPresiona R para jugar de nuevo");
        textoVictoria.setLocalTranslation(
                settings.getWidth() / 2f - 210f,
                settings.getHeight() / 2f + 50f,
                1f);
        textoVictoria.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(textoVictoria);

        BitmapFont fuente2 = assetManager.loadFont("Interface/Fonts/Default.fnt");

        textoLlaves = new BitmapText(fuente2, false);
        textoLlaves.setSize(fuente2.getCharSet().getRenderedSize() * 1.4f);
        textoLlaves.setColor(ColorRGBA.Yellow);
        textoLlaves.setText("Llaves: 0/2");
        textoLlaves.setLocalTranslation(
                settings.getWidth() - 130f,
                settings.getHeight() - 10f,
                1f);
        guiNode.attachChild(textoLlaves);

        textoPromptLlave = new BitmapText(fuente2, false);
        textoPromptLlave.setSize(fuente2.getCharSet().getRenderedSize() * 1.2f);
        textoPromptLlave.setColor(ColorRGBA.White);
        textoPromptLlave.setText("Presiona E para recoger");
        textoPromptLlave.setLocalTranslation(
                settings.getWidth()  / 2f - 110f,
                settings.getHeight() / 2f - 40f,
                1f);
        textoPromptLlave.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(textoPromptLlave);
    }

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
        inputManager.addMapping("RecogerLlave",   new KeyTrigger(KeyInput.KEY_E));

        // Triggers de Dificultad
        inputManager.addMapping("Dificultad1",    new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Dificultad2",    new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Dificultad3",    new KeyTrigger(KeyInput.KEY_3));

        inputManager.addListener(this,
                "Adelante", "Atras", "Izquierda", "Derecha", "Saltar", "Correr", "Reiniciar",
                "MirarDerecha", "MirarIzquierda", "MirarArriba", "MirarAbajo",
                "PortalO", "PortalP", "RecogerLlave", "Dificultad1", "Dificultad2", "Dificultad3");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (enMenuDificultad) {
            if (isPressed) {
                if (name.equals("Dificultad1")) {
                    walkSpeed = 7.5f; 
                    runSpeed = 11.0f;
                    finalizarSeleccionDificultad();
                } else if (name.equals("Dificultad2")) {
                    walkSpeed = 5.0f; 
                    runSpeed = 9.0f;
                    finalizarSeleccionDificultad();
                } else if (name.equals("Dificultad3")) {
                    walkSpeed = 2.5f;
                    runSpeed = 4.5f;  
                    finalizarSeleccionDificultad();
                }
            }
            return; 
        }

        if (name.equals("Reiniciar") && isPressed) {
            if (gameOver || juegoGanado) reiniciarJuego();
            return;
        }
        if (gameOver || juegoGanado) return;

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
                    if (portalB != null) portalB = new Portal(assetManager, rootNode, posicionP, posicionO.clone(), COLOR_PAR_1);
                }
                break;
            case "PortalP":
                if (isPressed) {
                    posicionP = playerControl.getPhysicsLocation().clone();
                    if (portalB != null) rootNode.detachChild(portalB.getNodo());
                    Vector3f destB = (posicionO != null) ? posicionO.clone() : posicionP.clone();
                    portalB = new Portal(assetManager, rootNode, posicionP, destB, COLOR_PAR_1);
                    if (portalA != null) portalA = new Portal(assetManager, rootNode, posicionO, posicionP.clone(), COLOR_PAR_1);
                }
                break;
            case "RecogerLlave":
                if (isPressed) {
                    intentarRecogerLlave();
                    intentarAbrirCajaFuerte();
                }
                break;
        }
    }

    private void finalizarSeleccionDificultad() {
        enMenuDificultad = false;
        guiNode.detachChild(overlayMenu);
        guiNode.detachChild(textoMenu);
        guiNode.detachChild(tituloJuego);
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (enMenuDificultad || gameOver || juegoGanado) return;
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

    @Override
    public void simpleUpdate(float tpf) {
        
        if (isRunning &&
            (moveForward || moveBackward || moveLeft || moveRight)) {

            if (audioLatidos.getStatus() != AudioSource.Status.Playing) {
                audioLatidos.play();
            }

        } else {

            if (audioLatidos.getStatus() == AudioSource.Status.Playing) {
                audioLatidos.stop();
            }

        }
        
        tiempoRisa += tpf;

            if (tiempoRisa >= siguienteRisa) {

                audioRisa.playInstance();

                tiempoRisa = 0f;

                // Entre 10 y 25 segundos
                siguienteRisa = 10f + FastMath.nextRandomFloat() * 15f;
            }
        
        if (enMenuDificultad || gameOver || juegoGanado) return;

        float velActual = isRunning ? runSpeed : walkSpeed;
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

        if (portalA != null) portalA.actualizar(tpf);
        if (portalB != null) portalB.actualizar(tpf);
        if (portalFijo1 != null) portalFijo1.actualizar(tpf);
        if (portalFijo2 != null) portalFijo2.actualizar(tpf);

        if (!enTeleporte) {
            if (portalA != null && portalB != null) {
                if (portalA.jugadorDentro(playerPos)) {
                    playerControl.setPhysicsLocation(portalB.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                } else if (portalB.jugadorDentro(playerPos)) {
                    playerControl.setPhysicsLocation(portalA.getDestino().add(0, 0.5f, 0));
                    enTeleporte = true;
                }
            }

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
                            cargarMapa("pasillo");
                            playerControl.setPhysicsLocation(POS_PASILLO_A_EXPANDED.add(0, 0.5f, 0));
                            break;
                        case "grande":
                            cargarMapa("expanded");
                            playerControl.setPhysicsLocation(POS_EXPANDED_A_GRANDE.add(0, 0.5f, 0));
                            break;
                    }
                } else if (portalFijo2 != null && portalFijo2.jugadorDentro(playerPos)) {
                    enTeleporte = true;
                    switch (mapaActual) {
                        case "pasillo":
                            cargarMapa("expanded");
                            playerControl.setPhysicsLocation(SPAWN_EXPANDED.clone());
                            break;
                        case "expanded":
                            cargarMapa("grande");
                            playerControl.setPhysicsLocation(SPAWN_GRANDE.clone());
                            break;
                    }
                }
            }
        } else {
            boolean fueraOP =
                    (portalA == null || !portalA.jugadorDentro(playerPos)) &&
                            (portalB == null || !portalB.jugadorDentro(playerPos));
            boolean fueraFijos =
                    (portalFijo1 == null || !portalFijo1.jugadorDentro(playerPos)) &&
                            (portalFijo2 == null || !portalFijo2.jugadorDentro(playerPos));
            if (fueraOP && fueraFijos) enTeleporte = false;
        }

        nina.actualizar(tpf, playerPos, cam);

        boolean mostrarPrompt = false;
        Vector3f pj = playerControl.getPhysicsLocation();

        if (nodoLlave1 != null && pj.distance(nodoLlave1.getWorldTranslation()) <= 2f) {
            textoPromptLlave.setText("Presiona E para recoger llave");
            mostrarPrompt = true;
        } else if (nodoLlave2 != null && pj.distance(nodoLlave2.getWorldTranslation()) <= 2f) {
            textoPromptLlave.setText("Presiona E para recoger llave");
            mostrarPrompt = true;
        } else if (nodoCajaFuerte != null && pj.distance(nodoCajaFuerte.getWorldTranslation()) <= 2.5f) {
            if (llavesRecogidas >= 2) {
                textoPromptLlave.setText("Presiona E para liberar el alma");
            } else {
                textoPromptLlave.setText("Necesitas las 2 llaves para abrir la caja");
            }
            mostrarPrompt = true;
        }

        textoPromptLlave.setCullHint(mostrarPrompt ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
    }

    private void activarGameOver() {
        gameOver = true;
        nina.ocultar();
        playerControl.setWalkDirection(Vector3f.ZERO);
        moveForward = moveBackward = moveLeft = moveRight = false;
        overlayRojo.setCullHint(Spatial.CullHint.Inherit);
        textoGameOver.setCullHint(Spatial.CullHint.Inherit);

        // Disparar sonido de muerte / jumpscare
        if (audioMuerte != null) {
            audioMuerte.play();
        }
    }

    private void activarVictoria() {
        juegoGanado = true;
        nina.ocultar();
        playerControl.setWalkDirection(Vector3f.ZERO);
        moveForward = moveBackward = moveLeft = moveRight = false;
        overlayVerde.setCullHint(Spatial.CullHint.Inherit);
        textoVictoria.setCullHint(Spatial.CullHint.Inherit);
        textoPromptLlave.setCullHint(Spatial.CullHint.Always);
    }

    private void reiniciarJuego() {
        gameOver = false;
        juegoGanado = false;

        overlayRojo.setCullHint(Spatial.CullHint.Always);
        textoGameOver.setCullHint(Spatial.CullHint.Always);
        overlayVerde.setCullHint(Spatial.CullHint.Always);
        textoVictoria.setCullHint(Spatial.CullHint.Always);

        // Detener sonido de muerte por si acaso se reinicia rápido
        if (audioMuerte != null) {
            audioMuerte.stop();
        }

        llave1Recogida  = false;
        llave2Recogida  = false;
        llavesRecogidas = 0;
        textoLlaves.setText("Llaves: 0/2");

        if (nodoLlave1 != null) { rootNode.detachChild(nodoLlave1); nodoLlave1 = null; }
        if (nodoLlave2 != null) { rootNode.detachChild(nodoLlave2); nodoLlave2 = null; }
        if (nodoCajaFuerte != null) { rootNode.detachChild(nodoCajaFuerte); nodoCajaFuerte = null; }

        cargarMapa("hospital"); 

        playerControl.setPhysicsLocation(SPAWN_POS.clone());
        playerControl.setWalkDirection(Vector3f.ZERO);
        yaw   = 0f;
        pitch = 0f;

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