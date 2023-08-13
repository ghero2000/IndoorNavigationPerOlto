package com.example.indoornavigation;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.OnMatrixChangedListener;
import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


/**
 * MainActivity è la classe principale dell'applicazione IndoorNavigationSolution.
 * Questa classe si occupa di caricare l'immagine della planimetria e di gestire il grafo
 * per la navigazione indoor, disegnando il percorso più breve tra due punti, sfruttando
 * le classi MapDrawer e Graph per la logica implementatiiva vera a propria.
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    private int stepCount = 0;
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
    private double x1 = 1027, x2 = 2578, x3 = 2370;
    private double y1 = 2450, y2 = 1810, y3 = 3180;
    private Handler handler;
    private int numSteps;

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
        path = null;

        graph.addNode("1", (float) 2020.6055 / 3520, (float) 1991.6936 / 4186, "atrium", "available", "notCrow");
        graph.addNode("1.1", (float) 2278.0957 / 3520, (float) 1913.4905 / 4186, "atrium", "available", "notCrow");
        graph.addNode("1.2", (float) 1769.668 / 3520, (float) 1773.3766 / 4186, "atrium", "available", "notCrow");

        graph.addNode("2", (float) 1965.1758 / 3520, (float) 2835.8684 / 4186, "atrium", "available", "notCrow");
        graph.addNode("2.1", (float) 1776.2207 / 3520, (float) 2523.056 / 4186, "atrium", "available", "notCrow");

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

        graph.addNode("9", (float) 2591.0156 / 3520, (float) 1913.4905 / 4186, "atrium", "available", "notCrow");

        graph.addEdge("1", "7", 1);    ////////////////////////////////////////// cancellare
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

        graph.addEdge("9", "1.1", 1);

        Graph.Node nodeA = graph.getNode("A");

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
                path = graph.findShortestPath(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd);
                try {
                    path.get(1);
                    path.get(2);
                } catch (Exception e) {
                    path = null;
                }
                if (path != null) {
                    disegnaPercorso(path);
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
        checkPoint(graph, touchTransformer, indicatorImage);
        handler = new Handler();
        startScanWithInterval();
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

    private void disegnaIndicatore(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        clearPath(indicatorImage);
        TouchTransformer transformer = new TouchTransformer();
        indicatorDrawer.drawIndicator((float) x1, (float) y1);
        indicatorDrawer.drawIndicator((float) x2, (float) y2);
        indicatorDrawer.drawIndicator((float) x3, (float) y3);
        indicatorDrawer.drawIndicator((float) x4, (float) y4, true);
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
                if (available == "available") {
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

    public void checkPoint(Graph graph, TouchTransformer touchTransformer, PhotoView indicatorImage) {
        indicatorImage.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                //879 1091
                float pointX = touchTransformer.transformX(x, indicatorImage, indicatorBitmap);
                float pointY = touchTransformer.transformY(y, indicatorImage, indicatorBitmap);
                //Toast.makeText(MainActivity.this, ""+pointX+"  "+pointY, Toast.LENGTH_SHORT).show();

                Graph.Node node = indoorNav.checkNode(graph, pointX, pointY);
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
                    if (node.getAvailability() == "available") {
                        sw_available.setChecked(true);
                    }
                    if (node.getAvailability() == "unavailable") {
                        sw_available.setChecked(false);
                    }
                    if (node.getCrowdness() == "crowded") {
                        sw_crowded.setChecked(true);
                    }
                    if (node.getCrowdness() == "notCrow") {
                        sw_crowded.setChecked(false);
                    }
                    sw_available.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (sw_available.isChecked()) {
                                node.setAvailability("available");
                            } else
                                node.setAvailability("unavailable");
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
                        disegnaIndicatore(x1, y1, x2, y2, x3, y3, position[0], position[1]);
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
                    disegnaIndicatore(x1, y1, x2, y2, x3, y3, position[0], position[1]);
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




