package com.example.indoornavigation;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.OnMatrixChangedListener;
import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * MainActivity è la classe principale dell'applicazione IndoorNavigationSolution.
 * Questa classe si occupa di caricare l'immagine della planimetria e di gestire il grafo
 * per la navigazione indoor, disegnando il percorso più breve tra due punti, sfruttando
 * le classi MapDrawer e Graph per la logica implementatiiva vera a propria.
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener, BeaconConsumer {
    private int stepCount = 0;

    private boolean loadingDismiss = true;

    private Dialog loadingDialog;

    // Lista per salvare le coordinate dei punti bianchi
    List<Coordinate> whitePoints = new ArrayList<>();

    List<Coordinate> unavailablePoints = new ArrayList<>();
    
    private TextView txt_dij, txt_aStar;
    private BeaconManager beaconManager;
    private Graph graphBackup = null;
    private int rssi1 = -1;
    private int rssi2 = -1;
    private int rssi3 = -1;

    private double distance1 = 0;
    private double distance2 = 0;
    private double distance3 = 0;
    private boolean showpath = false;
    private int steppy = 0;
    private boolean first = true;
    // ATTRIBUTI PER BUSSOLA
    private Graph.Node nodeSphere;

    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private List<String> macAddressList;
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private SimpleStepDetector simpleStepDetector;
    private ImageView compassImageView;
    private TextView degreeTextView;
    // ATTRIBUTI PER CONTAPASSI
    //private Sensor stepSensor;
    private Sensor orientationSensor;

    private TextView optTxt;

    private static final double referenceRSSI1 = -26.0;
    private static final double referenceRSSI2 = -22.0;

    private float[] position = new float[2];
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    // Un'istanza di PhotoView che visualizza l'immagine della planimetria
    PhotoView mapImage;
    PhotoView indicatorImage; //nuova photoView per l'indicatore utente.

    // Un'istanza di Bitmap che contiene l'immagine della planimetria
    private Bitmap mapBitmap;
    // Un'istanza di Bitmap che contiene l'immagine dell'indicatore
    private Bitmap indicatorBitmap;

    // Un'istanza di MapDrawer per disegnare percorsi sulla planimetria
    private MapDrawer mapDrawer;
    // Un'istanza di MapDrawer per disegnare indicatori utente sulla mappa
    private MapDrawer indicatorDrawer;

    private Map<String, Integer> uuidToRssiMap = new HashMap<>();
    private Map<String, Double> uuidToDistanceMap = new HashMap<>();

    private Drawable map;
    private Drawable indicator;

    private boolean[] start = new boolean[1];

    private int[] steps = new int[1];

    private double MagnitudePrevious = 0;
    private TextInputEditText startPoint;

    private TextInputEditText endPoint;

    private Switch aSwitch; //stairs

    private Switch bSwitch; //unavailable

    private Switch cSwitch; //crowded

    private String stairs = "";

    private String available = "";

    private String crowd = "";

    private Graph graph;

    private List<Graph.Node> path;

    private float currentRotation = 0f;

    private IndoorNavigation indoorNav;

    private Button drawBtn;

    private TouchTransformer touchTransformer;

    private Button btn_start;

    private TextView stepTextView;

    /*private double x1=971,x2=704,x3=761;
    private double y1=675,y2=925,y3=519;*/
    /*List<AccessPoint> accessPoints = new ArrayList<>();
                accessPoints.add(new AccessPoint(1027, 2450, 42));
                accessPoints.add(new AccessPoint(2589, 1853, 84));
                accessPoints.add(new AccessPoint(2252, 3100, 86));*/
    private Deque<SensorEvent> accelMeasurements = new ArrayDeque<>();
    private Deque<Double> filteredAccMagnitudes = new ArrayDeque<>();
    private boolean isStep = false;
    private long possibleStepTs = 0;
    private long stepTime = 0;
    private double stepLengthConst = 0.52;
    private double minimalThresholdValue = 0.05 * SensorManager.GRAVITY_EARTH;
    private long updateTimeIntervalMs = 700;
    private long minTimeBetweenStepsMs = 300;
    private int minimalNumberOfSteps = 5;
    private int maximumNumberOfSteps = 50;
    private int maxStepTime = 2000;
    private int averagingTimeIntervalMs = 2500;
    private int filterTimeIntervalMs = 200;
    private double x1 = 1027, x2 = 2578, x3 = 2370, x5 = 2268, x6 = 2004;
    private double y1 = 2450, y2 = 1810, y3 = 3180, y5 = 2599, y6 = 2930;
    private Handler handler;
    private int numSteps;
    private boolean safe;

    /**
     * Metodo onCreate per la creazione dell'activity.
     * Inizializza le variabili e carica l'immagine della planimetria.
     * Configura il grafo dei nodi e gli archi per la navigazione indoor.
     *
     * @param savedInstanceState Bundle contenente lo stato precedente dell'activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_dij = findViewById(R.id.txt_dijkstra);
        txt_aStar = findViewById(R.id.txt_astar);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        stepTextView = findViewById(R.id.txt_passi);
        stepTextView.setText("0");
        position[0] = 0;
        position[1] = 0;

        macAddressList = new ArrayList<>();

        startPoint = findViewById(R.id.starPoint);
        endPoint = findViewById(R.id.endPoint);

        btn_start = findViewById(R.id.btn_avvia);
        start[0] = false;

        map = getResources().getDrawable(R.drawable.casa_iuburesti);
        drawBtn = findViewById(R.id.drawBtn);
        mapBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.casa_iuburesti);

        indicator = getResources().getDrawable(R.drawable.indicator);
        indicatorBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.indicator);

        touchTransformer = new TouchTransformer();

        if (mapBitmap == null || indicatorBitmap == null) {
            Log.e("MainActivity", "Failed to load map image. " +
                    "Ensure that the image is present in the res/drawable folder " +
                    "and its name matches the one in the code.");
            return;
        }

        mapDrawer = new MapDrawer(mapBitmap);
        indicatorDrawer = new MapDrawer(indicatorBitmap);

        indoorNav = new IndoorNavigation(mapDrawer, getApplicationContext(), indicatorDrawer);

        //float[] touchPoint = new float[2];

        graph = new Graph(mapBitmap);
        graphBackup = new Graph(mapBitmap);
        path = null;

        /*
        graph.addNode("1.37",1393f, 2220f, "atrium", "available", "notCrow"); //1.4
        graph.addNode("1.38",1250f, 2210f, "atrium", "available", "notCrow"); //1.4
        graph.addNode("1.39",1093f, 2200f, "atrium", "available", "notCrow"); //1.4

        graph.addEdge("1.37", "1.38", 1);
        graph.addEdge("1.38", "1.39", 1);


        graph.addNode("1.4",1013f, 2745f, "atrium", "available", "notCrow"); //1.4
        graph.addNode("1.3",1170f, 2745f, "atrium", "available", "notCrow"); //1.3
        graph.addNode("1",1313f, 2745f, "atrium", "available", "notCrow"); //1
        graph.addNode("1.1",1470f, 2745f, "atrium", "available", "notCrow"); //1.1
        graph.addNode("1.2",1613f, 2745f, "atrium", "available", "notCrow"); //1.2

        graph.addNode("1.9",983f, 2885f, "atrium", "available", "notCrow"); //1.4
        graph.addNode("1.8",1140f, 2885f, "atrium", "available", "notCrow"); //1.3
        graph.addNode("1.5",1283f, 2885f, "atrium", "available", "notCrow"); //1
        graph.addNode("1.6",1440f, 2885f, "atrium", "available", "notCrow"); //1.1
        graph.addNode("1.7",1583f, 2885f, "atrium", "available", "notCrow"); //1.2

        graph.addNode("1.15",993f, 3025f, "atrium", "available", "notCrow");
        graph.addNode("1.14",1120f, 3025f, "atrium", "available", "notCrow");
        graph.addNode("1.11",1253f, 3025f, "atrium", "available", "notCrow");
        graph.addNode("1.12",1420f, 3025f, "atrium", "available", "notCrow");
        graph.addNode("1.13",1553f, 3025f, "atrium", "available", "notCrow");

        graph.addNode("1.19",1110f, 3165f, "atrium", "available", "notCrow");
        graph.addNode("1.16",1253f, 3165f, "atrium", "available", "notCrow");
        graph.addNode("1.17",1390f, 3165f, "atrium", "available", "notCrow");
        graph.addNode("1.18",1523f, 3165f, "atrium", "available", "notCrow");

        graph.addNode("1.25",1033f, 2605f, "atrium", "available", "notCrow");
        graph.addNode("1.24",1190f, 2605f, "atrium", "available", "notCrow");
        graph.addNode("1.21",1333f, 2605f, "atrium", "available", "notCrow");
        graph.addNode("1.22",1490f, 2605f, "atrium", "available", "notCrow");
        graph.addNode("1.23",1633f, 2605f, "atrium", "available", "notCrow");

        graph.addNode("1.31",1053f, 2465f, "atrium", "available", "notCrow");
        graph.addNode("1.29",1210f, 2465f, "atrium", "available", "notCrow");
        graph.addNode("1.26",1353f, 2465f, "atrium", "available", "notCrow");
        graph.addNode("1.27",1510f, 2465f, "atrium", "available", "notCrow");
        graph.addNode("1.28",1653f, 2465f, "atrium", "available", "notCrow");

        graph.addNode("1.36",1073f, 2325f, "atrium", "available", "notCrow");
        graph.addNode("1.35",1230f, 2325f, "atrium", "available", "notCrow");
        graph.addNode("1.32",1373f, 2325f, "atrium", "available", "notCrow");
        graph.addNode("1.33",1530f, 2325f, "atrium", "available", "notCrow");
        graph.addNode("1.34",1673f, 2325f, "atrium", "available", "notCrow");

        graph.addEdge("1.32", "1.33",1);
        graph.addEdge("1.33", "1.34",1);
        graph.addEdge("1.32", "1.35",1);
        graph.addEdge("1.36", "1.35",1);
        graph.addEdge("1.26", "1.32",1);
        graph.addEdge("1.26", "1.33",2);
        graph.addEdge("1.26", "1.35",2);
        graph.addEdge("1.27", "1.32",2);
        graph.addEdge("1.27", "1.33",1);
        graph.addEdge("1.27", "1.34",2);
        graph.addEdge("1.28", "1.34",1);
        graph.addEdge("1.28", "1.33",2);
        graph.addEdge("1.29", "1.32",2);
        graph.addEdge("1.29", "1.35",1);
        graph.addEdge("1.29", "1.36",2);
        graph.addEdge("1.31", "1.35",2);
        graph.addEdge("1.31", "1.36",1);

        graph.addEdge("1.21", "1.26", 1);
        graph.addEdge("1.21", "1.27", 2);
        graph.addEdge("1.21", "1.29", 2);
        graph.addEdge("1.22", "1.26", 2);
        graph.addEdge("1.22", "1.27", 1);
        graph.addEdge("1.22", "1.28", 2);
        graph.addEdge("1.23", "1.27", 2);
        graph.addEdge("1.23", "1.28", 1);
        graph.addEdge("1.24", "1.26", 2);
        graph.addEdge("1.24", "1.29", 1);
        graph.addEdge("1.24", "1.31", 2);
        graph.addEdge("1.25", "1.29", 2);
        graph.addEdge("1.25", "1.31", 1);

        graph.addEdge("1", "1.21", 1);
        graph.addEdge("1", "1.24", 2);
        graph.addEdge("1", "1.22", 2);
        graph.addEdge("1.1", "1.21", 2);
        graph.addEdge("1.1", "1.22", 1);
        graph.addEdge("1.1", "1.23", 2);
        graph.addEdge("1.2", "1.22", 2);
        graph.addEdge("1.2", "1.23", 1);
        graph.addEdge("1.3", "1.21", 2);
        graph.addEdge("1.3", "1.24", 1);
        graph.addEdge("1.3", "1.25", 2);
        graph.addEdge("1.4", "1.24", 2);
        graph.addEdge("1.4", "1.25", 1);

        graph.addEdge("1.11", "1.16", 1);
        graph.addEdge("1.11", "1.19", 2);
        graph.addEdge("1.11", "1.17", 2);
        graph.addEdge("1.12", "1.16", 2);
        graph.addEdge("1.12", "1.17", 1);
        graph.addEdge("1.12", "1.18", 2);
        graph.addEdge("1.13", "1.17", 2);
        graph.addEdge("1.13", "1.18", 1);
        graph.addEdge("1.14", "1.16", 2);
        graph.addEdge("1.14", "1.19", 1);
        graph.addEdge("1.15", "1.19", 2);
        graph.addEdge("1", "1.1", 1);
        graph.addEdge("1", "1.3", 1);
        graph.addEdge("1.2", "1.1", 1);
        graph.addEdge("1.4", "1.3", 1);
        graph.addEdge("1", "1.5", 1);
        graph.addEdge("1", "1.6", 2);
        graph.addEdge("1", "1.8", 2);
        graph.addEdge("1.1", "1.5", 2);
        graph.addEdge("1.1", "1.6", 1);
        graph.addEdge("1.1", "1.7", 2);
        graph.addEdge("1.2", "1.6", 2);
        graph.addEdge("1.2", "1.7", 1);
        graph.addEdge("1.3", "1.5", 2);
        graph.addEdge("1.3", "1.8", 1);
        graph.addEdge("1.3", "1.9", 2);
        graph.addEdge("1.4", "1.8", 2);
        graph.addEdge("1.4", "1.9", 1);
        graph.addEdge("1.5", "1.11", 1);
        graph.addEdge("1.5", "1.12", 2);
        graph.addEdge("1.5", "1.14", 2);
        graph.addEdge("1.6", "1.11", 2);
        graph.addEdge("1.6", "1.12", 1);
        graph.addEdge("1.6", "1.13", 1);
        graph.addEdge("1.7", "1.12", 2);
        graph.addEdge("1.7", "1.13", 1);
        graph.addEdge("1.8", "1.11", 2);
        graph.addEdge("1.8", "1.14", 1);
        graph.addEdge("1.8", "1.15", 2);
        graph.addEdge("1.9", "1.14", 2);
        graph.addEdge("1.9", "1.15", 1);

        graph.addEdge("1.5", "1.6", 1);
        graph.addEdge("1.6", "1.7", 1);
        graph.addEdge("1.5", "1.8", 1);
        graph.addEdge("1.8", "1.9", 1);
        graph.addEdge("1.11", "1.12", 1);
        graph.addEdge("1.12", "1.13", 1);
        graph.addEdge("1.12", "1.13", 1);
        graph.addEdge("1.11", "1.14", 1);
        graph.addEdge("1.15", "1.14", 1);
        graph.addEdge("1.16", "1.17", 1);
        graph.addEdge("1.16", "1.19", 1);
        graph.addEdge("1.17", "1.18", 1);
        graph.addEdge("1.21", "1.22", 1);
        graph.addEdge("1.21", "1.24", 1);
        graph.addEdge("1.22", "1.23", 1);
        graph.addEdge("1.24", "1.25", 1);
        graph.addEdge("1.26", "1.27", 1);
        graph.addEdge("1.26", "1.29", 1);
        graph.addEdge("1.27", "1.28", 1);
        graph.addEdge("1.29", "1.31", 1);
        graph.addEdge("1.32", "1.37", 1);
        graph.addEdge("1.32", "1.38", 2);
        graph.addEdge("1.33", "1.37", 2);
        graph.addEdge("1.35", "1.37", 2);
        graph.addEdge("1.35", "1.38", 1);
        graph.addEdge("1.35", "1.39", 2);
        graph.addEdge("1.36", "1.39", 1);
        graph.addEdge("1.36", "1.38", 2);



        graph.addNode("2", (float) 2147, (float) 2420, "atrium", "available", "notCrow");

        graph.addNode("2.1", (float) 1553, (float) 2363, "atrium", "available", "notCrow");

        graph.addNode("3", (float) 866.89453 / 3520, (float) 2128.549 / 4186, "classroom", "available", "notCrow");
        graph.addNode("3.1", (float) 1450.3027 / 3520, (float) 2089.4475 / 4186, "classroom", "available", "notCrow");

        graph.addNode("4", (float) 827.79297 / 3520, (float) 1600.678 / 4186, "bathroom", "available", "notCrow");
        graph.addNode("4.1", (float) 1029.8535 / 3520, (float) 1493.1487 / 4186, "bathroom", "available", "notCrow");

        graph.addNode("5", (float) 1342.7734 / 3520, (float) 909.651 / 4186, "classroom", "available", "notCrow");
        graph.addNode("5.1", (float) 1463.3008 / 3520, (float) 1209.4297 / 4186, "classroom", "available", "notCrow");

        graph.addNode("6", (float) 1939.1797 / 3520, (float) 883.5833 / 4186, "classroom", "available", "notCrow");
        graph.addNode("6.1", (float) 1763.1152 / 3520, (float) 1248.5312 / 4186, "classroom", "available", "notCrow");

        graph.addNode("7", (float) 2046.709 / 3520, (float) 1493.1487 / 4186, "stairs", "available", "notCrow");  ////////////////////////////////////////////////// modificare

        graph.addNode("8", (float) 1450.3027 / 3520, (float) 1519.2164 / 4186, "hallway", "available", "notCrow");
        graph.addNode("8.1", (float) 1450.3027 / 3520, (float) 1789.669 / 4186, "hallway", "available", "notCrow");
        graph.addNode("8.2", (float) 1776.2207 / 3520, (float) 1467.081 / 4186, "hallway", "available", "notCrow");

        graph.addNode("9", (float) 2591.0156 / 3520, (float) 1913.4905 / 4186, "atrium", "available", "notCrow"); */


    /*
        // Lista per salvare le coordinate dei punti neri
        List<Coordinate> blackPoints = new ArrayList<>();

        // Scansione dell'area rettangolare e salvataggio delle coordinate dei punti neri
        for (int y = 480; y < 3280; y=y+10) {
            for (int x = 815; x < 2875; x=x+10) {
                int pixelColor = mapBitmap.getPixel(x, y);
                int red = Color.red(pixelColor);
                int green = Color.green(pixelColor);
                int blue = Color.blue(pixelColor);

                // Esempio di controllo per determinare se il pixel è nero o simile
                if (red <= 200 && green <= 200 && blue <= 200) {
                    blackPoints.add(new Coordinate(x, y));
                }
            }
        }


        // Scansione dell'area rettangolare e salvataggio delle coordinate dei punti bianchi
        for (int y = 480; y < 3280; y += 10) {
            for (int x = 815; x < 2875; x += 10) {
                int pixelColor = mapBitmap.getPixel(x, y);
                int red = Color.red(pixelColor);
                int green = Color.green(pixelColor);
                int blue = Color.blue(pixelColor);

                // Controllo per determinare se il pixel è bianco o simile
                if (red > 200 && green > 200 && blue > 200) {
                    boolean isNearBlackPoint = false;

                    // Controlla se il punto bianco è vicino a un punto nero
                    for (Coordinate blackPoint : blackPoints) {
                        int dx = Math.abs(x - blackPoint.x);
                        int dy = Math.abs(y - blackPoint.y);

                        if (dx <= 20 && dy <= 20) {
                            isNearBlackPoint = true;
                            break;  // Esci dal ciclo una volta trovato un punto nero vicino
                        }
                    }

                    if (!isNearBlackPoint) {
                        whitePoints.add(new Coordinate(x, y));
                        graph.addNode(x + "-" + y, x, y, "atrium", "available", "notCrow");
                        graphBackup.addNode(x + "-" + y, x, y, "atrium", "available", "notCrow");
                        // Creazione del JSON per il nodo corrente
                    }
                }
            }
        } */

        /*
        try {
            // Ottieni il contenuto del file JSON dalla cartella "assets"
            InputStream inputStream = getAssets().open("nodes.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            // Converti il contenuto in una stringa JSON
            String json = new String(buffer, "UTF-8");

            // Parse del JSON
            JSONObject jsonObject = new JSONObject(json);

            // Ciclo attraverso gli oggetti nel JSON e aggiungi i nodi al grafo
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject nodeJson = jsonObject.getJSONObject(key);

                int x = nodeJson.getInt("x");
                int y = nodeJson.getInt("y");
                String roomType = nodeJson.getString("roomType");
                String availability = nodeJson.getString("availability");
                String crowdness = nodeJson.getString("crowdness");

                // Aggiungi il nodo al grafo
                whitePoints.add(new Coordinate(x, y));
                graph.addNode(key, x, y, roomType, availability, crowdness);
                graphBackup.addNode(key, x, y, roomType, availability, crowdness);
            }
        } catch (IOException | JSONException e) {
            Toast.makeText(this, "ciauzomega", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } */


        try {
            // Ottieni il contenuto del file JSON dalla cartella "assets"
            InputStream inputStream = getAssets().open("nodi.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            // Converti il contenuto in una stringa JSON
            String json = new String(buffer, "UTF-8");

            // Parse del JSON
            JSONObject jsonObject = new JSONObject(json);

            // Ciclo attraverso gli oggetti nel JSON e aggiungi i nodi al grafo
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject nodeJson = jsonObject.getJSONObject(key);

                int x = nodeJson.getInt("x");
                int y = nodeJson.getInt("y");
                String roomType = nodeJson.getString("roomType");
                String availability = nodeJson.getString("availability");
                String crowdness = nodeJson.getString("crowdness");

                // Aggiungi il nodo al grafo
                whitePoints.add(new Coordinate(x,y));
                graph.addNode(key, x, y, roomType, availability, crowdness);
                graphBackup.addNode(key, x, y, roomType, availability, crowdness);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        for (Coordinate coord : whitePoints) {
            int x = coord.x;
            int y = coord.y;
            String nodeId = x + "-" + y;

            for (int dy = -10; dy <= 10; dy += 10) {
                for (int dx = -10; dx <= 10; dx += 10) {
                    if (dx == 0 && dy == 0) {
                        continue;  // Salta il nodo stesso
                    }

                    int adjacentX = x + dx;
                    int adjacentY = y + dy;
                    String adjacentNodeId = adjacentX + "-" + adjacentY;
                    //Log.d("cieck", ""+adjacentNodeId);

                    String startNodeId = x + "-" + y;

                    // Verifica che il nodo associato alle coordinate sia presente nella lista dei nodi
                    if (graph.getNode(startNodeId) != null && graph.getNode(adjacentNodeId) != null) {
                        // Aggiungi l'arco tra i nodi
                        graph.addEdge(startNodeId, adjacentNodeId, 1);
                        //Toast.makeText(MainActivity.this, ""+startNodeId+"-"+adjacentNodeId, Toast.LENGTH_SHORT).show();
                        graphBackup.addEdge(startNodeId, adjacentNodeId, 1);
                        safe = true;
                        //Log.d("procopio", ""+adjacentNodeId);
                    }
                    else {
                        Log.d("procopio", "ciao");
                    }
                }
            }
        }

        /*
        // Ottieni un'istanza del database
        JSONObject jsonz = new JSONObject();

        // Scrittura dei dati JSON
        for (Coordinate coord : whitePoints) {
            String nodeId = coord.x + "-" + coord.y;
            JSONObject jsonNode = new JSONObject();
            try {
                jsonNode.put("x", coord.x);
                jsonNode.put("y", coord.y);
                jsonNode.put("roomType", "atrium");
                jsonNode.put("availability", "available");
                jsonNode.put("crowdness", "notCrow");

                // Scrivi i dati nel database utilizzando il nodeId come chiave
                jsonz.put(nodeId, jsonNode);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        try {
            // Definisci il percorso completo della cartella di destinazione
            File destinationFolder = new File(Environment.getExternalStorageDirectory(), "Download");
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }

            // Crea il percorso completo per il file desiderato
            File outputFile = new File(destinationFolder, "nodi.json");

            // Scrivi i dati nel file
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(jsonz.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } */

        /*
        // Ottieni un'istanza del database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("nodes");

        // Scrittura dei dati JSON
        for (Coordinate coord : whitePoints) {
            String nodeId = coord.x + "-" + coord.y;
            JSONObject jsonNode = new JSONObject();
            try {
                jsonNode.put("x", coord.x);
                jsonNode.put("y", coord.y);
                jsonNode.put("roomType", "atrium");
                jsonNode.put("availability", "available");
                jsonNode.put("crowdness", "notCrow");

                // Scrivi i dati nel database utilizzando il nodeId come chiave
                databaseReference.child(nodeId).setValue(jsonNode.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } */


        /*
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("nodes");

        // Aggiungi un listener per leggere i dati
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot nodeSnapshot : dataSnapshot.getChildren()) {
                    String nodeId = nodeSnapshot.getKey();
                    String jsonNodeString = nodeSnapshot.getValue(String.class);

                    // Esegui il parsing del JSON e aggiungi il nodo al grafo
                    try {
                        JSONObject jsonNode = new JSONObject(jsonNodeString);
                        int x = jsonNode.getInt("x");
                        int y = jsonNode.getInt("y");
                        String roomType = jsonNode.getString("roomType");
                        String availability = jsonNode.getString("availability");
                        String crowdness = jsonNode.getString("crowdness");

                        // Aggiungi il nodo al grafo
                        whitePoints.add(new Coordinate(x, y));
                        graph.addNode(nodeId, x, y, roomType, availability, crowdness);
                        graphBackup.addNode(nodeId, x, y, roomType, availability, crowdness);

                        // Aggiungi eventuali archi o altre operazioni desiderate
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Gestisci l'errore
            }
        }); */

        /*graph.addEdge("1", "7", 1);    ////////////////////////////////////////// cancellare
        graph.addEdge("7", "6", 1);    ////////////////////////////////////////// cancellare

        graph.addEdge("1", "1.1", 1);
        graph.addEdge("1", "1.2", 1);

        graph.addEdge("2", "2.1", 1);

        graph.addEdge("3", "3.1", 1);

        graph.addEdge("4", "4.1", 1);

        graph.addEdge("5", "5.1", 1);

        graph.addEdge("6", "6.1", 1);

        graph.addEdge("8", "8.1", 1);

        graph.addEdge("8", "8.2", 1);

        graph.addEdge("1", "2.1", 1);

        graph.addEdge("1.2", "2.1", 1);
        graph.addEdge("1.2", "8.1", 1);

        graph.addEdge("3.1", "8.1", 1);

        graph.addEdge("4.1", "8", 1);

        graph.addEdge("5.1", "8", 1);

        graph.addEdge("6.1", "8.2", 1);

        graph.addEdge("7", "8.2", 1);

        graph.addEdge("9", "1.1", 1); */

        mapImage = findViewById(R.id.map_image);
        mapImage.setImageDrawable(map);
        mapImage.setImageBitmap(mapDrawer.getMapBitmap());
        mapImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        indicatorImage = findViewById(R.id.indicator_image);
        indicatorImage.setImageDrawable(indicator);
        indicatorImage.setImageBitmap(indicatorDrawer.getMapBitmap());
        indicatorImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);


        optTxt = findViewById(R.id.btn_options);
        checkOptions();
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodeSphere = indoorNav.stepNavigation(path, mapImage, steppy, indicatorImage, start);
                steppy++;
                if (start[0]) {
                    start[0] = false;
                    disegnaIndicatore(0, 0);
                } else {
                    if (showpath) {
                        start[0] = true;
                        btn_start.setVisibility(View.GONE);
                    }
                }
            }
        });

        drawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPath();
                //path = graph.findShortestPathACO(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd, 10, 100, 0.1, 1.0, 2.0, 100.0);
                double startTime = System.currentTimeMillis();
                //path = graph.findShortestPath(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd);
                //Toast.makeText(MainActivity.this, ""+path.size(), Toast.LENGTH_SHORT).show();
                double endTime = System.currentTimeMillis();   // Timestamp finale
                double elapsedTime = endTime - startTime;
                //txt_dij.setText(elapsedTime+"");
                startTime = System.currentTimeMillis();
                try {
                    path = graph.findShortestPathAStar(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd);
                    //path = graph.findShortestPathACO(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd, 10, 100);
                    //Toast.makeText(MainActivity.this, ""+graph.getNode("1085-2710").getCrowdness(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    //
                }
                Toast.makeText(MainActivity.this, ""+path.size(), Toast.LENGTH_SHORT).show();
                endTime = System.currentTimeMillis();   // Timestamp finale
                elapsedTime = endTime - startTime;
                txt_aStar.setText(elapsedTime+"");
                //path = graph.findShortestPath(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd, 5, 150, 0.1, 10.0, 20.0, 100.0);
                //Toast.makeText(MainActivity.this, ""+elapsedTime, Toast.LENGTH_SHORT).show();
                //disegnaTutto(whitePoints);
                for (Graph.Node node : path) {
                    Log.d("Node ID", node.getId());
                }
                try {
                    path.get(0);
                    path.get(1);
                } catch (Exception e) {
                    path = null;
                }
                if (path != null) {
                    disegnaIndicatore(whitePoints, path);
                    showpath = true;
                    steppy = 0;
                }
            }
        });

        Log.d("Coordinate", "Width: " + String.valueOf(mapBitmap.getWidth()) + "  Height: " + String.valueOf(mapBitmap.getHeight()));

        indicatorImage.setOnMatrixChangeListener(new OnMatrixChangedListener() {
            @Override
            public void onMatrixChanged(RectF rect) {
                Matrix matrix = new Matrix();
                indicatorImage.getSuppMatrix(matrix);
                mapImage.setDisplayMatrix(matrix);
            }
        });

        Button stepBtn = findViewById(R.id.stepBtn);

        stepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPath(true);
                nodeSphere = indoorNav.stepNavigation(path, mapImage, steppy, indicatorImage, start);
                steppy++;
                if (nodeSphere == null) {
                    btn_start.setVisibility(View.VISIBLE);
                    showpath = false;
                    stepTextView.setText("0");
                }
            }
        });

        //onCreate per bussola
        compassImageView = findViewById(R.id.compass_image_view);
        degreeTextView = findViewById(R.id.degree_text_view);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //---------- fine bussola
        //onCreate per contapassi
        //stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);
        steps[0] = 0;

        checkAndRequestPermissions();
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        checkPoint(graph, touchTransformer, indicatorImage, whitePoints);
        handler = new Handler();
        startScanWithInterval();
        // Avvia il thread per eseguire checkCrowd() in background con un loop
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    checkCrowd(whitePoints);
                    if (!loadingDismiss) {
                        loadingDismiss = true;
                        loadingDialog.dismiss();
                    }
                    try {
                        Thread.sleep(6000); // Attendi 3 secondi
                    } catch (InterruptedException e) {
                        // Gestisci l'interruzione del thread, se necessario
                        Thread.currentThread().interrupt(); // Reimposta il flag interrotto
                    }
                }
            }
        });
        thread.start();
    }

    private void checkCrowd(List<Coordinate> whitePoints) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("coordinate");
        //Toast.makeText(this, ""+databaseReference.getKey(), Toast.LENGTH_SHORT).show();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //Toast.makeText(MainActivity.this, "esisti", Toast.LENGTH_SHORT).show();
                    long x = dataSnapshot.child("x").getValue(Long.class);
                    long y = dataSnapshot.child("y").getValue(Long.class);

                    // Ora hai i valori x e y, puoi procedere con il disegno.
                    updateCrowdedness(x, y, 110);
                    if(startPoint.getText().toString() != null &&
                            endPoint.getText().toString() != null && path != null){
                        path = graph.findShortestPathAStar(startPoint.getText().toString(),
                                endPoint.getText().toString(), stairs, available, crowd);
                    }
                    disegnaIndicatore(x, y, 110, true, whitePoints, path);
                    disegnaTutto(whitePoints);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Gestisci l'errore
            }
        });

    }

    private void updateCrowdedness(long x, long y, int radius) {

        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.x);
            int dy = Math.abs((int) y - coord.y);

            graph.getNode(coord.x+"-"+coord.y).setCrowdness(graphBackup.getNode(coord.x+"-"+coord.y).getCrowdness());

            if (dx * dx + dy * dy <= radius * radius) {
                // Nodo all'interno del raggio
                try {
                    graph.getNode(coord.x + "-" + coord.y).setCrowdness("crowded");
                } catch (Exception e) {

                }
            }
        }
    }

    private void updateAvailability(long x, long y, int radius, boolean b) {

        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.x);
            int dy = Math.abs((int) y - coord.y);

            //graph.getNode(coord.x+"-"+coord.y).setAvailability(graphBackup.getNode(coord.x+"-"+coord.y).getAvailability());

            if (dx * dx + dy * dy <= radius * radius) {
                // Nodo all'interno del raggio
                try {
                    if (b)
                        graph.getNode(coord.x + "-" + coord.y).setAvailability("available");
                    else
                        graph.getNode(coord.x + "-" + coord.y).setAvailability("unavailable");
                } catch (Exception e) {

                }
            }
        }
    }

    private void disegnaTutto(List<Coordinate> whitePoints) {
        for (Coordinate point : whitePoints) {
            if (graph.getNode(point.x+"-"+ point.y).getAvailability().equals("unavailable")) {
                mapDrawer.drawIndicator(point.x, point.y, true, 5);
            }
        }
        /*
        indicatorDrawer.drawIndicator(1013f, 2745f, true, 20);
        indicatorDrawer.drawIndicator(1170f, 2745f, true, 20);
        indicatorDrawer.drawIndicator(1313f, 2745f, true, 20); //1
        indicatorDrawer.drawIndicator(1470f, 2745f, true, 20);
        indicatorDrawer.drawIndicator(1613f, 2745f, true, 20);

        indicatorDrawer.drawIndicator(983f, 2885f, true, 20);
        indicatorDrawer.drawIndicator(1140f, 2885f, true, 20);
        indicatorDrawer.drawIndicator(1283f, 2885f, true, 20); //1
        indicatorDrawer.drawIndicator(1440f, 2885f, true, 20);
        indicatorDrawer.drawIndicator(1583f, 2885f, true, 20);

        indicatorDrawer.drawIndicator(993f, 3025f, true, 20);
        indicatorDrawer.drawIndicator(1120f, 3025f, true, 20);
        indicatorDrawer.drawIndicator(1253f, 3025f, true, 20); //1
        indicatorDrawer.drawIndicator(1420f, 3025f, true, 20);
        indicatorDrawer.drawIndicator(1553f, 3025f, true, 20);

        indicatorDrawer.drawIndicator(1110f, 3165f, true, 20);
        indicatorDrawer.drawIndicator(1253f, 3165f, true, 20); //1
        indicatorDrawer.drawIndicator(1390f, 3165f, true, 20);
        indicatorDrawer.drawIndicator(1523f, 3165f, true, 20);

        indicatorDrawer.drawIndicator(1033f, 2605f, true, 20);
        indicatorDrawer.drawIndicator(1190f, 2605f, true, 20);
        indicatorDrawer.drawIndicator(1333f, 2605f, true, 20); //1
        indicatorDrawer.drawIndicator(1490f, 2605f, true, 20);
        indicatorDrawer.drawIndicator(1633f, 2605f, true, 20);

        indicatorDrawer.drawIndicator(1053f, 2465f, true, 20);
        indicatorDrawer.drawIndicator(1210f, 2465f, true, 20);
        indicatorDrawer.drawIndicator(1353f, 2465f, true, 20); //1
        indicatorDrawer.drawIndicator(1510f, 2465f, true, 20);
        indicatorDrawer.drawIndicator(1653f, 2465f, true, 20);

        indicatorDrawer.drawIndicator(1073f, 2325f, true, 20);
        indicatorDrawer.drawIndicator(1230f, 2325f, true, 20);
        indicatorDrawer.drawIndicator(1373f, 2325f, true, 20); //1
        indicatorDrawer.drawIndicator(1530f, 2325f, true, 20);
        indicatorDrawer.drawIndicator(1673f, 2325f, true, 20);

        indicatorDrawer.drawIndicator(1093f, 2200f, true, 20);
        indicatorDrawer.drawIndicator(1250f, 2210f, true, 20);
        indicatorDrawer.drawIndicator(1393f, 2220f, true, 20); //1

        indicatorDrawer.drawIndicator(1734f, 2404f, true, 20);
        indicatorDrawer.drawIndicator(2147f, 2420f, true, 20);
        indicatorDrawer.drawIndicator(2304f, 2440f, true, 20); //2
        indicatorDrawer.drawIndicator(2461f, 2460f, true, 20);
        indicatorDrawer.drawIndicator(2618f, 2480f, true, 20); */
    }

    private void startScanWithInterval() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainWifi.startScan();
                handler.postDelayed(this, 7000); // Programma il prossimo aggiornamento dopo 1 secondo
            }
        }, 500); // Avvia la scansione dopo 1 secondo dalla creazione dell'Activity
    }

    private void disegnaIndicatore(float x, float y) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        clearPath(indicatorImage);
        TouchTransformer transformer = new TouchTransformer();
        indicatorDrawer.drawIndicator(x, y);
        indicatorImage.invalidate();
        Matrix newMatrix = new Matrix();
        newMatrix.setScale(currentScale, currentScale);
        newMatrix.postTranslate(currentTranslate.x, currentTranslate.y);
        indicatorImage.setDisplayMatrix(newMatrix);
    }

    private void disegnaIndicatore(long x, long y, int i, boolean b, List<Coordinate> whitePoints, List<Graph.Node> nodes) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        disegnaPercorso(path);
        clearPath();
        showpath = true;
        steppy = 0;
        mapDrawer.drawIndicator(x, y, i, b);
        mapDrawer.drawPath(nodes, mapImage, true);
        //disegnaTutto(whitePoints);
        mapImage.invalidate();
        Matrix newMatrix = new Matrix();
        newMatrix.setScale(currentScale, currentScale);
        newMatrix.postTranslate(currentTranslate.x, currentTranslate.y);
        indicatorImage.setDisplayMatrix(newMatrix);
    }

    private void disegnaIndicatore(List<Coordinate> whitePoints, List<Graph.Node> nodes) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        disegnaPercorso(path);
        clearPath();
        showpath = true;
        steppy = 0;
        mapDrawer.drawPath(nodes, mapImage, true);
        //disegnaTutto(whitePoints);
        mapImage.invalidate();
        Matrix newMatrix = new Matrix();
        newMatrix.setScale(currentScale, currentScale);
        newMatrix.postTranslate(currentTranslate.x, currentTranslate.y);
        indicatorImage.setDisplayMatrix(newMatrix);
    }

    private void disegnaIndicatore(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double dist1, double dist2, double dist3) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        clearPath(indicatorImage);
        //TouchTransformer transformer = new TouchTransformer();
        indicatorDrawer.drawIndicator((float) x1, (float) y1); //ex x6 y6
        indicatorDrawer.drawIndicator((float) x2, (float) y2); //ex x5 y5
        indicatorDrawer.drawIndicator((float) x3, (float) y3);
        indicatorDrawer.drawIndicator((float) x4, (float) y4, true, 60);
        indicatorDrawer.drawIndicator((float) x1, (float) y1, false, dist1  * 220);
        indicatorDrawer.drawIndicator((float) x2, (float) y2, false, dist2  * 220);
        indicatorDrawer.drawIndicator((float) x3, (float) y3, false, dist3  * 220);
        indicatorImage.invalidate();
        Matrix newMatrix = new Matrix();
        newMatrix.setScale(currentScale, currentScale);
        newMatrix.postTranslate(currentTranslate.x, currentTranslate.y);
        indicatorImage.setDisplayMatrix(newMatrix);
    }

    private void checkOptions() {
        optTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog2 = new Dialog(MainActivity.this);
                //  Imposta il layout del tuo dialog personalizzato
                dialog2.setContentView(R.layout.options_dialog);

                aSwitch = dialog2.findViewById(R.id.switch1);
                bSwitch = dialog2.findViewById(R.id.switch2);
                cSwitch = dialog2.findViewById(R.id.switch3);

                if (stairs == "stairs") {
                    aSwitch.setChecked(true);
                } else aSwitch.setChecked(false);
                if (available == "unavailable") {
                    bSwitch.setChecked(true);
                } else bSwitch.setChecked(false);
                if (crowd == "crowded") {
                    cSwitch.setChecked(true);
                } else cSwitch.setChecked(false);

                aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (aSwitch.isChecked())
                            stairs = "stairs";
                        else
                            stairs = "";
                    }
                });

                bSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (bSwitch.isChecked())
                            available = "unavailable";
                        else
                            available = "";
                    }
                });

                cSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (cSwitch.isChecked())
                            crowd = "crowded";
                        else
                            crowd = "";
                    }
                });
                dialog2.show();
            }
        });
    }

    /**
     * Disegna il percorso più breve tra due nodi sulla planimetria.
     * Utilizza l'istanza di MapDrawer per disegnare il percorso sulla planimetria
     * e aggiorna l'immagine visualizzata.
     *
     * @param nodes Lista di nodi che rappresentano il percorso da disegnare
     */
    private void disegnaPercorso(List<Graph.Node> nodes) {
        mapDrawer.drawPath(nodes, mapImage, true);
        mapImage.invalidate();
    }

    public void clearPath() {
        mapDrawer.resetMap(); // Aggiungi questa riga per ripristinare la mappa nel MapDrawer
        mapImage.setImageBitmap(mapDrawer.getMapBitmap()); // Imposta la nuova mappa ripristinata
        mapImage.invalidate(); // Forza il ridisegno della PhotoView
    }

    public void clearPath(boolean b) {
        mapImage.invalidate(); // Forza il ridisegno della PhotoView
    }

    public void clearPath(PhotoView image) {
        indicatorDrawer.resetMap(); // Aggiungi questa riga per ripristinare la mappa nel MapDrawer
        image.setImageBitmap(indicatorDrawer.getMapBitmap()); // Imposta la nuova mappa ripristinata
        image.invalidate(); // Forza il ridisegno della PhotoView
    }

    public void checkPoint(Graph graph, TouchTransformer touchTransformer, PhotoView indicatorImage, List<Coordinate> whitePoints) {
        indicatorImage.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                //879 1091
                float pointX = touchTransformer.transformX(x, indicatorImage, indicatorBitmap);
                float pointY = touchTransformer.transformY(y, indicatorImage, indicatorBitmap);
                //Toast.makeText(MainActivity.this, pointX+"  "+pointY, Toast.LENGTH_SHORT).show();

                Graph.Node node = indoorNav.checkNode(graph, pointX, pointY, whitePoints);
                final Dialog dialog = new Dialog(MainActivity.this);

                //  Imposta il layout del tuo dialog personalizzato
                dialog.setContentView(R.layout.custom_dialog);

                TextView node_name = dialog.findViewById(R.id.node_name);
                TextView node_id = dialog.findViewById(R.id.node_id);
                TextView node_type = dialog.findViewById(R.id.node_type);

                if (node != null) {
                    node_name.setText("Node: " + node.getId());
                    node_id.setText(node.getId());
                    node_type.setText(node.getRoomType());

                    Button btn_starting = dialog.findViewById(R.id.start_btn);
                    Button btn_end = dialog.findViewById(R.id.end_btn);

                    Switch sw_crowded = dialog.findViewById(R.id.sw_crowded);
                    Switch sw_available = dialog.findViewById(R.id.sw_available);

                    btn_starting.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startPoint.setText(node_id.getText().toString());
                            dialog.dismiss();
                        }
                    });

                    btn_end.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            endPoint.setText(node_id.getText().toString());
                            dialog.dismiss();
                        }
                    });
                    if (node.getAvailability().equals("available")) {
                        sw_available.setChecked(true);
                    }
                    if (node.getAvailability().equals("unavailable")) {
                        sw_available.setChecked(false);
                    }
                    if (node.getAvailability().equals("crowded")) {
                        sw_crowded.setChecked(true);
                    }
                    if (node.getAvailability().equals("notCrow")) {
                        sw_crowded.setChecked(false);
                    }
                    sw_available.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (sw_available.isChecked()) {
                                dialog.dismiss();
                                loadingDialog = new Dialog(MainActivity.this);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                                loadType.setText("Rimuovendo L'Ostacolo");
                                loadingDialog.show();
                                for (Coordinate coord: unavailablePoints) {
                                    if(Math.abs(coord.x - node.getX()) < 220 && Math.abs(coord.y - node.getY()) < 220) {
                                        unavailablePoints.remove(coord);
                                        updateAvailability(coord.x, coord.y, 110, true);
                                        loadingDismiss = false;
                                        break;
                                    }
                                }
                            } else {
                                node.setAvailability("unavailable");
                                dialog.dismiss();
                                loadingDialog = new Dialog(MainActivity.this);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                                loadType.setText("Aggiungendo L'Ostacolo");
                                loadingDialog.show();
                                updateAvailability((long) node.getX(), (long) node.getY(), 110, false);
                                unavailablePoints.add(new Coordinate((int) node.getX(), (int) node.getY()));
                                if(startPoint.getText().toString() != null &&
                                        endPoint.getText().toString() != null && path != null){
                                    path = graph.findShortestPathAStar(startPoint.getText().toString(),
                                            endPoint.getText().toString(), stairs, available, crowd);
                                }
                                loadingDismiss = false;
                            }
                        }
                    });

                    sw_crowded.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (sw_crowded.isChecked()) {
                                node.setCrowdness("crowded");
                            } else
                                node.setCrowdness("notCrow");
                        }
                    });

                    // Mostra il dialog
                    dialog.show();
                }
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        } else {
            // Permission already granted, proceed with getting WiFi scan results
            startBeaconScanning();
        }
    }

    private void startBeaconScanning() {
        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

        // Definisci i layout dei beacon che vuoi rilevare
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.bind(this);

        // Aggiungi un RangeNotifier per gestire i risultati della scansione
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // Gestisci i risultati della scansione qui
            }
        });

        try {
            // Avvia la scansione dei beacon nella regione specificata
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private static double calculateDistanceFromRSSI(double rssi, boolean b) {
        //double weight = true ? Math.abs(rssi - referenceRSSI1) : 1.0;
        // Calcola il rapporto tra la potenza del segnale ricevuto e la potenza a un metro di distanza (in scala logaritmica)
        if (b) {
            double ratio = (referenceRSSI1 - rssi) / 20.0;
            double distance = Math.pow(10, ratio);
            return distance;
        }
        else {
            double ratio = (referenceRSSI2 - rssi) / 20.0;
            double distance = Math.pow(10, ratio);
            return distance;
        }

        // Calcola la distanza in metri utilizzando il modello di Friis
        // D = 10 ^ (ratio), dove D è la distanza in metri

    }

    public AbstractMap.SimpleEntry<Integer, Integer> findNearestReferencePoint(int rssi1, int rssi2, int rssi3) {
        // Crea un oggetto Fingerprint e ottieni i Reference Points
        Fingerprint fingerprint = new Fingerprint();
        List<Fingerprint.ReferencePoint> referencePoints = fingerprint.getReferencePoints();

        // Inizializza una variabile per memorizzare la minima differenza RSSI e il RP associato
        int minDifference = Integer.MAX_VALUE;
        Fingerprint.ReferencePoint nearestReferencePoint = null;

        // Itera attraverso i Reference Points per trovare quello con i valori RSSI più vicini
        for (Fingerprint.ReferencePoint referencePoint : referencePoints) {
            int difference1 = Math.abs(rssi1 - referencePoint.getRssi1());
            int difference2 = Math.abs(rssi2 - referencePoint.getRssi2());
            int difference3 = Math.abs(rssi3 - referencePoint.getRssi3());

            // Calcola la somma delle differenze
            int totalDifference = difference1 + difference2 + difference3;

            // Se la somma delle differenze è minore di quella attualmente minima,
            // aggiorna la variabile con il nuovo RP più vicino
            if (totalDifference < minDifference) {
                minDifference = totalDifference;
                nearestReferencePoint = referencePoint;
            }
        }

        // Ora hai il RP più vicino con i valori RSSI corrispondenti
        if (nearestReferencePoint != null) {
            int x = nearestReferencePoint.getX();
            int y = nearestReferencePoint.getY();
            return new AbstractMap.SimpleEntry<>(x, y);
        } else {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        numSteps = 0;
        stepTextView.setText("" + numSteps);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        unregisterReceiver(receiverWifi);
    }

    /*
    private Magnitude calculateFilteredAccMagnitudes() {
        int numSamples = 10; // Numero di campioni da utilizzare per calcolare la media
        double sum = 0;

        int count = Math.min(numSamples, accelMeasurements.size());
        Iterator<SensorEvent> iterator = accelMeasurements.descendingIterator();

        for (int i = 0; i < count; i++) {
            SensorEvent event = iterator.next();
            double magnitude = Math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]
            );
            sum += magnitude;
        }

        double average = count > 0 ? sum / count : 0;
        long now = System.currentTimeMillis();
        return new Magnitude(now, average);
    } */


    /*
    private void updateStepCount() {
        if (!isStep) {
            if (filteredAccMagnitudes.size() < minimalNumberOfSteps) {
                return;
            }

            int n = filteredAccMagnitudes.size();
            double maxMagnitude = 0.0;
            for (double magnitude : filteredAccMagnitudes) {
                if (magnitude > maxMagnitude) {
                    maxMagnitude = magnitude;
                }
            }

            if (maxMagnitude < minimalThresholdValue) {
                filteredAccMagnitudes.clear();
                return;
            }

            // Find peaks in the magnitude data
            int stepsDetected = 0;
            Iterator<Double> iterator = filteredAccMagnitudes.iterator();
            long firstTimestamp = accelMeasurements.getFirst().timestamp;
            for (int i = 0; i < n - 1; i++) {
                double prevMagnitude = iterator.next();
                double currentMagnitude = iterator.next();
                double nextMagnitude = iterator.next();

                if (currentMagnitude > prevMagnitude && currentMagnitude > nextMagnitude) {
                    long ts = firstTimestamp + (long) (i * filterTimeIntervalMs);
                    if (ts - possibleStepTs >= minTimeBetweenStepsMs) {
                        possibleStepTs = ts;
                        stepTime = ts;
                        stepsDetected++;
                        if (stepsDetected == minimalNumberOfSteps) {
                            Log.d("idolatra", "stepsDetected"+stepsDetected);
                            isStep = true;
                            break;
                        }
                    }
                }
            }

            // Remove old magnitude data
            while (filteredAccMagnitudes.size() > maximumNumberOfSteps) {
                filteredAccMagnitudes.removeFirst();
            }

            // Update the step count in the TextView
            if (isStep) {
                int stepCount = filteredAccMagnitudes.size();
                stepTextView.setText(String.valueOf(stepCount));
                isStep = false;
            }
        }
    } */


    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
            mGravity[0] = mGravity[0] - event.values[0];
            mGravity[1] = mGravity[1] - event.values[1];
            mGravity[2] = mGravity[2] - event.values[2];

            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);

            float x_acceleration = event.values[0];
            float y_acceleration = event.values[1];
            float z_acceleration = event.values[2];
            /*
            double Magnitude = Math.sqrt(x_acceleration*x_acceleration + y_acceleration*y_acceleration + z_acceleration*z_acceleration);
            double MagnitudeDelta = Magnitude - MagnitudePrevious;
            MagnitudePrevious = Magnitude;

            if (MagnitudeDelta > 6){
                stepCount++;
                //isStep = true;
            } */
            //stepTextView.setText(String.valueOf(stepCount)); //old accelerometer (forse devo tenerlo)
            /*
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                accelMeasurements.add(event);
                Magnitude magn = calculateFilteredAccMagnitudes();
                if (magn.getTs() != 0) {
                    //Log.d("idolatra", "getTs  "+magn.getTs());
                    filteredAccMagnitudes.add(magn.getValue());
                    //Log.d("idolatra", "magnavalore   "+magn.getValue());
                }

                updateStepCount();
            }
        } */

        /*
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Update step count when a step is detected
            stepCount = (int) event.values[0];
            stepTextView.setText(stepCount+""); */
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
        }

        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
            float degrees = (azimuthInDegrees + 360) % 360;

            degreeTextView.setText(String.format(Locale.getDefault(), "%.0f°", degrees));
            compassImageView.setRotation(-degrees);
            if (true) {
                /*mapImage.setRotation(degrees);
                indicatorImage.setRotation(degrees);*/
                double radian = Math.toRadians(degrees);
                double stepLength = 50;
                //Toast.makeText(this, ""+isStep, Toast.LENGTH_SHORT).show();
                if (isStep) {
                    isStep = false;
                    double deltaX = stepLength * Math.sin(radian);
                    double deltaY = stepLength * Math.cos(radian);
                    if (position[0] != 0) {
                        position[0] -= deltaX;
                        position[1] -= deltaY;
                        //Toast.makeText(this, ""+Toast[0], Toast.LENGTH_SHORT).show();
                        try {
                            disegnaIndicatore(x1, y1, x2, y2, x3, y3, position[0], position[1],
                                    uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa1"),
                                    uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa2"),
                                    uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"));
                        } catch (Exception e) {
                            //
                        }
                        /*double dx = Math.abs(Toast[0]-nodeSphere.getX());
                        double dy = Math.abs(Toast[1]-nodeSphere.getY());
                        if (dx < 50 && dy < 50) {
                            clearPath(true);
                            nodeSphere = indoorNav.stepNavigation(path, mapImage, steppy, indicatorImage, start);
                            steppy ++;
                            if (nodeSphere == null) {
                                btn_start.setVisibility(View.VISIBLE);
                                showpath = false;
                                stepTextView.setText("0");
                                return;
                            }
                        } */
                    }
                }
            }
            else {
                mapImage.setRotation(0f);
                indicatorImage.setRotation(0f);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // niente
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        isStep = true;
        stepTextView.setText("" + numSteps);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                if (beacons.isEmpty()) {
                    // Aggiungi valore di default quando non vengono trovati beacon
                } else {
                    for (Beacon beacon : beacons) {
                        String uuid = beacon.getId1().toString();
                        int rssi = beacon.getRssi();
                        double distance = beacon.getDistance();


                        uuidToRssiMap.put(uuid, rssi);
                        uuidToDistanceMap.put(uuid, distance);

                        DecimalFormat df = new DecimalFormat("#.##");
                        String formattedDistance = df.format(distance);

                        String beaconData = "UUID: " + uuid + "\nRSSI: " + rssi + "\nDistance: " + formattedDistance + " meters";
                    }

                    // Esempio di chiamata a calculateUserPosition utilizzando le mappe
                    /*
                    calculateUserPosition(uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa1"),
                            uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa2"),
                            uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"),
                            uuidToRssiMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa1"),
                            uuidToRssiMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa2"),
                            uuidToRssiMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6")); */
                    disegnaIndicatore(x1, y1, x2, y2, x3, y3, position[0], position[1],
                            uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa1"),
                            uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa2"),
                            uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"));
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            if (ActivityCompat.checkSelfPermission(c, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            wifiList = mainWifi.getScanResults();
            distance1 = 0; distance2 = 0; distance3 = 0;
            rssi1 = -1;
            rssi2 = -1;
            rssi3 = -1;
            String macAddressToFind = "e0:b9:e5:55:45:53";
            String macAddressToFind2 = "e2:b9:e5:55:45:5b";
            String macAddressToFind3 = "5c:62:8b:32:7b:91";
            String macAddressToFind4 = "30:de:4b:56:73:37";
            if (wifiList != null) {
                for (ScanResult scanResult : wifiList) {
                    if (scanResult.BSSID.equals(macAddressToFind) || scanResult.BSSID.equals(macAddressToFind2)) {
                        rssi1 = scanResult.level;
                        distance1 = calculateDistanceFromRSSI(rssi1, true);
                        break;
                    }
                }
            }
            if (wifiList != null) {
                for (ScanResult scanResult : wifiList) {
                    if (scanResult.BSSID.equals(macAddressToFind3)) {
                        rssi2 = scanResult.level;
                        distance2 = calculateDistanceFromRSSI(rssi2, true);
                        break;
                    }
                }
            }
            if (wifiList != null) {
                for (ScanResult scanResult : wifiList) {
                    if (scanResult.BSSID.equals(macAddressToFind4)) {
                        rssi3 = scanResult.level;
                        distance3 = calculateDistanceFromRSSI(rssi3, true);
                        break;
                    }
                }
            }
            //Toast.makeText(MainActivity.this, ""+rssi1+"  "+rssi2+"  "+rssi3+" "+pointX+" "+pointY, Toast.LENGTH_SHORT).show();
            /*double A = 2 * (x2 - x1);
            double B = 2 * (y2 - y1);
            double C = Math.pow(distance1, 2) - Math.pow(distance2, 2) - Math.pow(x1, 2) + Math.pow(x2, 2) - Math.pow(y1, 2) + Math.pow(y2, 2);
            double D = 2 * (x3 - x2);
            double E = 2 * (y3 - y2);
            double F = Math.pow(distance2, 2) - Math.pow(distance3, 2) - Math.pow(x2, 2) + Math.pow(x3, 2) - Math.pow(y2, 2) + Math.pow(y3, 2);

            double x4 = (C * E - F * B) / (E * A - B * D);
            double y4 = (C * D - A * F) / (B * D - A * E);
            disegnaIndicatore(x1, y1, x2, y2, x3, y3, x4, y4); */

            if (rssi1 != -1 && rssi2 != -1 && rssi3 != -1) {
                AbstractMap.SimpleEntry<Integer, Integer> nearestRP = findNearestReferencePoint(rssi1, rssi2, rssi3);

                if (nearestRP != null) {
                    // Estrai i valori x e y dalla coppia
                    double x4 = nearestRP.getKey();
                    double y4 = nearestRP.getValue();
                    position[0] = (float) x4;
                    position[1] = (float) y4;
                    try {
                        disegnaIndicatore(x1, y1, x2, y2, x3, y3, position[0], position[1],
                                uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa1"),
                                uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa2"),
                                uuidToDistanceMap.get("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"));
                    } catch (Exception e) {
                        //
                    }
                } else {
                    //
                }
            }

            //Log.d("iddiota", pointX+" "+pointY+" "+rssi1+" "+rssi2+" "+rssi3);
            //Toast.makeText(MainActivity.this, rssi1+" "+rssi2+" "+rssi3, Toast.LENGTH_SHORT).show();
        }
    }

    private static class Magnitude {
        private long ts;
        private double value;

        public Magnitude(long ts, double value) {
            this.ts = ts;
            this.value = value;
        }

        public long getTs() {
            return ts;
        }

        public double getValue() {
            return value;
        }
    }
}




