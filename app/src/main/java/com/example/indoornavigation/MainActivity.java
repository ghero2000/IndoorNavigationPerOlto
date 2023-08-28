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
import android.os.Handler;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
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
    List<Coordinate> crowdedPoints = new ArrayList<>();
    List<Coordinate> stairPoints = new ArrayList<>();
    List<Coordinate> elevatorPoints = new ArrayList<>();
    List<Coordinate> unavailablePoints2 = new ArrayList<>();
    List<Coordinate> crowdedPoints2 = new ArrayList<>();
    List<Coordinate> stairPoints2 = new ArrayList<>();
    List<Coordinate> elevatorPoints2 = new ArrayList<>();

    private TextView txt_dij, txt_aStar;
    private ImageView btn_up, btn_down;
    private boolean floor = false; //false = primo piano, true = secondo
    private BeaconManager beaconManager;
    private Graph graphBackup1 = null;
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

    private AutoCompleteTextView startPoint;

    private AutoCompleteTextView endPoint;

    private Switch aSwitch; //stairs

    private Switch bSwitch; //unavailable

    private Switch cSwitch; //crowded

    private String stairs = "";

    private String available = "";

    private String crowd = "";

    private Graph graph1;
    private Graph graph2;

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
    private Graph graphBackup2 = null;

    private ListView startSuggestionListView;
    private ListView endSuggestionListView;

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

        btn_up = findViewById(R.id.btn_up);
        btn_down = findViewById(R.id.btn_down);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        stepTextView = findViewById(R.id.txt_passi);
        stepTextView.setText("0");
        position[0] = 0;
        position[1] = 0;

        macAddressList = new ArrayList<>();

        startPoint = findViewById(R.id.startPoint);
        endPoint = findViewById(R.id.endPoint);

        final ListView startSuggestionListView = findViewById(R.id.startSuggestionListView);
        final ListView endSuggestionListView = findViewById(R.id.endSuggestionListView);

        String[] suggestions = {"Secondo Piano", "Primo Piano", "Suggerimento", "Suggerimento", "Suggerimento", "Suggerimento"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, suggestions) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                String suggestion = suggestions[position];

                // Creazione di un ClickableSpan per la parola cliccabile
                SpannableString spannableString = new SpannableString(suggestion);
                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        // Azione da eseguire quando l'utente fa clic sulla parola cliccabile
                        endPoint.setText(suggestion); // Imposta il testo nell'AutoCompleteTextView
                        endSuggestionListView.setVisibility(View.GONE); // Nasconde la lista dei suggerimenti
                    }
                };
                spannableString.setSpan(clickableSpan, 0, suggestion.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                textView.setText(spannableString);
                textView.setMovementMethod(LinkMovementMethod.getInstance()); // Rendi il testo cliccabile

                return textView;
            }
        };

        endPoint.setAdapter(adapter);

        endPoint.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedSuggestion = (String) parent.getItemAtPosition(position);
                endPoint.setText(selectedSuggestion); // Imposta il testo selezionato nell'AutoCompleteTextView
                endPoint.setVisibility(View.GONE); // Nasconde la lista dei suggerimenti
            }
        });

        endPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 // Mostra la lista dei suggerimenti
                endSuggestionListView.setVisibility(View.VISIBLE);
            }
        });

        /*
        // Creazione di un hint cliccabile con SpannableString
        String hint = "Secondo Piano";
        SpannableString spannableString = new SpannableString(hint);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                // Azione da eseguire quando l'utente fa clic sull'hint cliccabile
                endPoint.setText(hint);
            }
        };
        spannableString.setSpan(clickableSpan, 0, hint.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

        endLayout.setHint(spannableString);
        endLayout.setHintAnimationEnabled(true); // Per animare l'effetto di hint cliccabile */

        btn_start = findViewById(R.id.btn_avvia);
        start[0] = false;

        map = getResources().getDrawable(R.drawable.casa_iubirii);
        drawBtn = findViewById(R.id.drawBtn);
        mapBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.casa_iubirii);

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

        graph1 = new Graph(mapBitmap);
        graphBackup1 = new Graph(mapBitmap);
        path = null;

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
                whitePoints.add(new Coordinate(x,y,floor));
                graph1.addNode(key, x, y, roomType, availability, crowdness);
                if (roomType.equals("stairs")) {
                    //Toast.makeText(this, "staravia", Toast.LENGTH_SHORT).show();
                    stairPoints.add(new Coordinate(x, y,floor));
                }
                if (roomType.equals("elevator")) {
                    //Toast.makeText(this, "staravia", Toast.LENGTH_SHORT).show();
                    elevatorPoints.add(new Coordinate(x, y,floor));
                }
                graphBackup1.addNode(key, x, y, roomType, availability, crowdness);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        for (Coordinate coord : whitePoints) {
            int x = coord.getX();
            int y = coord.getY();
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
                    if (graph1.getNode(startNodeId) != null && graph1.getNode(adjacentNodeId) != null) {
                        // Aggiungi l'arco tra i nodi
                        graph1.addEdge(startNodeId, adjacentNodeId, 1);
                        //Toast.makeText(MainActivity.this, ""+startNodeId+"-"+adjacentNodeId, Toast.LENGTH_SHORT).show();
                        graphBackup1.addEdge(startNodeId, adjacentNodeId, 1);
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
                if (startPoint.getText().toString().equals("") || endPoint.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "Inserisci i dati correttamente", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                    if(floor)
                        path = graph2.findShortestPathAStar(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd, floor);
                    else
                        path = graph1.findShortestPathAStar(startPoint.getText().toString(), endPoint.getText().toString(), stairs, available, crowd, floor);
                } catch (Exception e) {
                    //
                }
                endTime = System.currentTimeMillis();   // Timestamp finale
                elapsedTime = endTime - startTime;
                txt_aStar.setText(elapsedTime+"");
                try {
                    path.get(0);
                    path.get(1);
                } catch (Exception e) {
                    path = null;
                }
                if (path != null) {
                    loadingDialog = new Dialog(MainActivity.this);
                    loadingDialog.setContentView(R.layout.loading_dialog);
                    TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                    loadType.setText("Calcolo Del Percorso...");
                    loadingDialog.show();
                    checkCrowd(whitePoints);
                }
            }
        });

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

        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!floor) {
                    floor = true;
                    if (endPoint.getText().toString().equals("2255-1140") || path.get(path.size()-1).getId().equals("2255-1140")) {
                        startPoint.setText("A2255-1140");
                    }
                    if (endPoint.getText().toString().equals("2175-1990") || path.get(path.size()-1).getId().equals("2175-1990")) {
                        startPoint.setText("A2175-1990");
                    }
                    if (endPoint.getText().toString().equals("1385-2700") || path.get(path.size()-1).getId().equals("1385-2700")) {
                        startPoint.setText("A1385-2700");
                    }
                    if (endPoint.getText().toString().equals("2215-3070") || path.get(path.size()-1).getId().equals("2255-3070")) {
                        startPoint.setText("A2215-3070");
                    }
                    path = null;
                    whitePoints.clear();
                    map = null;
                    map = getResources().getDrawable(R.drawable.casa_iubirii2);
                    mapBitmap = null;
                    mapBitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.casa_iubirii2);
                    mapDrawer = new MapDrawer(mapBitmap);
                    indicatorDrawer = new MapDrawer(indicatorBitmap);
                    mapImage = null;
                    mapImage = findViewById(R.id.map_image);
                    // Puoi reimpostare l'immagine a null per "resettare" il PhotoView
                    mapImage.setImageResource(0); // Oppure
                    mapImage.setImageDrawable(null);
                    mapImage.setImageDrawable(map);
                    mapImage.setImageBitmap(mapDrawer.getMapBitmap());
                    mapImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    //mapImage.invalidate();

                    if (graph2 == null) {
                        graph2 = new Graph(mapBitmap);
                        graphBackup2 = new Graph(mapBitmap);
                        // Lista per salvare le coordinate dei punti neri
                        List<Coordinate> blackPoints = new ArrayList<>();

                        // Scansione dell'area rettangolare e salvataggio delle coordinate dei punti neri
                        for (int y = 480; y < 3280; y = y + 10) {
                            for (int x = 815; x < 2875; x = x + 10) {
                                int pixelColor = mapBitmap.getPixel(x, y);
                                int red = Color.red(pixelColor);
                                int green = Color.green(pixelColor);
                                int blue = Color.blue(pixelColor);

                                // Esempio di controllo per determinare se il pixel è nero o simile
                                if (red <= 200 && green <= 200 && blue <= 200) {
                                    blackPoints.add(new Coordinate(x, y,floor));
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
                                        int dx = Math.abs(x - blackPoint.getX());
                                        int dy = Math.abs(y - blackPoint.getY());

                                        if (dx <= 20 && dy <= 20) {
                                            isNearBlackPoint = true;
                                            break;  // Esci dal ciclo una volta trovato un punto nero vicino
                                        }
                                    }

                                    if (!isNearBlackPoint) {
                                        whitePoints.add(new Coordinate(x, y,floor));
                                        graph2.addNode("A"+x + "-" + y, x, y, "atrium", "available", "notCrow");
                                        graphBackup2.addNode("A"+x + "-" + y, x, y, "atrium", "available", "notCrow");
                                    }
                                }
                            }
                        }
                        for (Coordinate coord : whitePoints) {
                            int x = coord.getX();
                            int y = coord.getY();

                            for (int dy = -10; dy <= 10; dy += 10) {
                                for (int dx = -10; dx <= 10; dx += 10) {
                                    if (dx == 0 && dy == 0) {
                                        continue;  // Salta il nodo stesso
                                    }

                                    int adjacentX = x + dx;
                                    int adjacentY = y + dy;
                                    String adjacentNodeId = "A"+adjacentX + "-" + adjacentY;
                                    //Log.d("cieck", ""+adjacentNodeId);

                                    String startNodeId = "A"+x + "-" + y;

                                    // Verifica che il nodo associato alle coordinate sia presente nella lista dei nodi
                                    if (graph2.getNode(startNodeId) != null && graph2.getNode(adjacentNodeId) != null) {
                                        // Aggiungi l'arco tra i nodi
                                        graph2.addEdge(startNodeId, adjacentNodeId, 1);
                                        //Toast.makeText(MainActivity.this, ""+startNodeId+"-"+adjacentNodeId, Toast.LENGTH_SHORT).show();
                                        graphBackup2.addEdge(startNodeId, adjacentNodeId, 1);
                                        safe = true;
                                        //Log.d("procopio", ""+adjacentNodeId);
                                    } else {
                                        Log.d("procopio", "ciao");
                                    }
                                }
                            }
                        }
                    }
                    else {
                        // Lista per salvare le coordinate dei punti neri
                        List<Coordinate> blackPoints = new ArrayList<>();

                        // Scansione dell'area rettangolare e salvataggio delle coordinate dei punti neri
                        for (int y = 480; y < 3280; y = y + 10) {
                            for (int x = 815; x < 2875; x = x + 10) {
                                int pixelColor = mapBitmap.getPixel(x, y);
                                int red = Color.red(pixelColor);
                                int green = Color.green(pixelColor);
                                int blue = Color.blue(pixelColor);

                                // Esempio di controllo per determinare se il pixel è nero o simile
                                if (red <= 200 && green <= 200 && blue <= 200) {
                                    blackPoints.add(new Coordinate(x, y,floor));
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
                                        int dx = Math.abs(x - blackPoint.getX());
                                        int dy = Math.abs(y - blackPoint.getY());

                                        if (dx <= 20 && dy <= 20) {
                                            isNearBlackPoint = true;
                                            break;  // Esci dal ciclo una volta trovato un punto nero vicino
                                        }
                                    }

                                    if (!isNearBlackPoint) {
                                        whitePoints.add(new Coordinate(x, y,floor));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (floor) {
                    floor = false;
                    path = null;
                    whitePoints.clear();
                    map = null;
                    map = getResources().getDrawable(R.drawable.casa_iubirii);
                    mapBitmap = null;
                    mapBitmap = BitmapFactory.decodeResource(getResources(),
                            R.drawable.casa_iubirii);
                    mapDrawer = new MapDrawer(mapBitmap);
                    indicatorDrawer = new MapDrawer(indicatorBitmap);
                    mapImage = null;
                    mapImage = findViewById(R.id.map_image);
                    // Puoi reimpostare l'immagine a null per "resettare" il PhotoView
                    mapImage.setImageResource(0); // Oppure
                    mapImage.setImageDrawable(null);
                    mapImage.setImageDrawable(map);
                    mapImage.setImageBitmap(mapDrawer.getMapBitmap());
                    mapImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    //mapImage.invalidate();
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

                            // Aggiungi il nodo al grafo
                            whitePoints.add(new Coordinate(x, y,floor));
                        }
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        checkAndRequestPermissions();
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        if (floor)
            checkPoint(graph2, touchTransformer, indicatorImage, whitePoints);
        else checkPoint(graph1, touchTransformer, indicatorImage, whitePoints);
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

                    updateCrowdedness(x, y, 110);
                    clearPath();
                    disegnaIndicatoreThread(x, y, 110, true, whitePoints, path);
                    for (Coordinate point : crowdedPoints) {
                        if (point.isFloor() == floor)
                            disegnaIndicatoreThread(point.getX(), point.getY(), 110, true, whitePoints, path);
                    }

                    for (Coordinate point : unavailablePoints) {
                        if (point.isFloor() == floor)
                            disegnaIndicatoreThread(point.getX(), point.getY(), 110, false, whitePoints, path);
                    }
                    try {
                        loadingDialog.dismiss();
                    } catch (Exception e) {
                        // il loading dialog non esiste?
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Gestisci l'errore
            }
        });

    }

    /*
    private void updateCrowdedness(long x, long y, int radius) {

        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.x);
            int dy = Math.abs((int) y - coord.y);

            Graph graph;
            Graph graphBackup;
            if(floor) {
                graph = graph2;
                graphBackup = graphBackup2;
            }
            else {
                graph = graph1;
                graphBackup = graphBackup1;
            }

            try {
                graph.getNode(coord.x + "-" + coord.y).setCrowdness(graphBackup.getNode(coord.x + "-" + coord.y).getCrowdness());
                graph.getNode(coord.x + "-" + coord.y).setFixed(false);
            } catch (Exception e) {

            }

            try {
                graph.getNode("A"+coord.x + "-" + coord.y).setCrowdness(graphBackup.getNode("A"+coord.x + "-" + coord.y).getCrowdness());
                graph.getNode("A"+coord.x + "-" + coord.y).setFixed(false);
            } catch (Exception e) {

            }

            for (Coordinate coordCrowded: crowdedPoints) {
                try {
                    graph.getNode(coordCrowded.x + "-" + coordCrowded.y).setCrowdness("crowded");
                } catch (Exception e) {

                }

                try {
                    graph.getNode("A"+coordCrowded.x + "-" + coordCrowded.y).setCrowdness("crowded");
                } catch (Exception e) {

                }

                int dxRadius = Math.abs(coordCrowded.x - coord.x);
                int dyRadius = Math.abs(coordCrowded.y - coord.y);
                if (dxRadius * dxRadius + dyRadius * dyRadius <= radius * radius) {
                    try {
                        graph.getNode(coord.x + "-" + coord.y).setCrowdness("crowded");
                    } catch (Exception e) {

                    }
                    try {
                        graph.getNode("A"+coord.x + "-" + coord.y).setCrowdness("crowded");
                    } catch (Exception e) {

                    }
                }
            }

            if (dx * dx + dy * dy <= radius * radius) {
                // Nodo all'interno del raggio
                try {
                    graph.getNode(coord.x + "-" + coord.y).setCrowdness("crowded");
                    graph.getNode(coord.x + "-" + coord.y).setFixed(true);
                } catch (Exception e) {

                }
                try {
                    graph.getNode("A"+coord.x + "-" + coord.y).setCrowdness("crowded");
                    graph.getNode("A"+coord.x + "-" + coord.y).setFixed(true);
                } catch (Exception e) {

                }
            }
        }
    } */

    private void updateCrowdedness(long x, long y, int radius) {
        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.getX());
            int dy = Math.abs((int) y - coord.getY());

            Graph graph = floor ? graph2 : graph1;
            Graph graphBackup = floor ? graphBackup2 : graphBackup1;

            String nodeKey = floor ? "A" + coord.getX() + "-" + coord.getY() : coord.getX() + "-" + coord.getY();
            Graph.Node node = graph.getNode(nodeKey);
            Graph.Node backupNode = graphBackup.getNode(nodeKey);

            try {
                node.setCrowdness(backupNode.getCrowdness());
                node.setFixed(false);
            } catch (Exception e) {}

            for (Coordinate coordCrowded : crowdedPoints) {
                String crowdedNodeKey = coordCrowded.isFloor() ? "A" + coordCrowded.getX() + "-" + coordCrowded.getY() : coordCrowded.getX() + "-" + coordCrowded.getY();
                Graph.Node crowdedNode = graph.getNode(crowdedNodeKey);

                try {
                    if (coordCrowded.isFloor() == floor) {
                        crowdedNode.setCrowdness("crowded");
                    }
                } catch (Exception e) {}

                int dxRadius = Math.abs(coordCrowded.getX() - coord.getX());
                int dyRadius = Math.abs(coordCrowded.getY() - coord.getY());
                if (dxRadius * dxRadius + dyRadius * dyRadius <= radius * radius) {
                    try {
                        if (coordCrowded.isFloor() == floor)
                            node.setCrowdness("crowded");
                    } catch (Exception e) {}
                }
            }
        }
    }


    private void updateAvailability(long x, long y, int radius, boolean b) {

        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.getX());
            int dy = Math.abs((int) y - coord.getY());

            //graph.getNode(coord.x+"-"+coord.y).setAvailability(graphBackup.getNode(coord.x+"-"+coord.y).getAvailability());

            if (dx * dx + dy * dy <= radius * radius) {
                // Nodo all'interno del raggio
                try {
                    Graph graph;
                    if(floor) graph = graph2;
                    else graph = graph1;
                    if (b) {
                        try {
                            graph.getNode(coord.getX() + "-" + coord.getY()).setAvailability("available");
                        } catch (Exception e) {

                        }
                        try {
                            graph.getNode("A"+ coord.getX() + "-" + coord.getY()).setAvailability("available");
                        } catch (Exception e) {

                        }
                    }
                    else {
                        try {
                            graph.getNode(coord.getX() + "-" + coord.getY()).setAvailability("unavailable");
                        } catch (Exception e) {

                        }
                        try {
                            graph.getNode("A"+ coord.getX() + "-" + coord.getY()).setAvailability("unavailable");
                        } catch (Exception e) {

                        }
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    private void updateSelfCrowd(long x, long y, int radius, boolean b) {

        for (Coordinate coord : whitePoints) {
            int dx = Math.abs((int) x - coord.getX());
            int dy = Math.abs((int) y - coord.getY());

            //graph.getNode(coord.x+"-"+coord.y).setAvailability(graphBackup.getNode(coord.x+"-"+coord.y).getAvailability());

            if (dx * dx + dy * dy <= radius * radius) {
                // Nodo all'interno del raggio
                try {
                    Graph graph;
                    if(floor) graph = graph2;
                    else graph = graph1;
                    if (b) {
                        try {
                            graph.getNode(coord.getX() + "-" + coord.getY()).setCrowdness("notCrow");
                        } catch (Exception e) {

                        }
                        try {
                            graph.getNode("A"+ coord.getX() + "-" + coord.getY()).setCrowdness("notCrow");
                        } catch (Exception e) {

                        }
                    }
                    else {
                        try {
                            graph.getNode("A"+ coord.getX() + "-" + coord.getY()).setCrowdness("crowded");
                        } catch (Exception e) {

                        }
                        try {
                            graph.getNode(coord.getX() + "-" + coord.getY()).setCrowdness("crowded");
                        } catch (Exception e) {

                        }
                    }
                } catch (Exception e) {

                }
            }
        }
    }

    private void disegnaTutto(List<Coordinate> whitePoints, boolean b) {
        Graph graph;
        if (floor) graph = graph2;
        else graph = graph1;
        for (Coordinate point : whitePoints) {
            if (graph.getNode(point.getX() +"-"+ point.getY()).getAvailability().equals("unavailable")) {
                mapDrawer.drawIndicator(point.getX(), point.getY(), true, 5);
            }
            if (graph.getNode(point.getX() +"-"+ point.getY()).getCrowdness().equals("crowded") && b) {
                mapDrawer.drawIndicator(point.getX(), point.getY(), true, 6);
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

    private void disegnaScala(int x, int y, Graph graph) {
        if (floor)
            if (!graph.getNode("A"+x+"-"+y).getRoomType().equals("stairs")) {
                graph.getNode("A"+x+"-"+y).setRoomType("stairs");
            }
        mapDrawer.drawStair(x, y, getApplicationContext());
    }
    private void disegnaAscensore(int x, int y, Graph graph) {
        if (floor)
            if (!graph.getNode("A"+x+"-"+y).getRoomType().equals("elevator")){
                graph.getNode("A"+x+"-"+y).setRoomType("elevator");
            }
        mapDrawer.drawElevator(x, y);
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

    private void disegnaIndicatoreThread(long x, long y, int i, boolean b, List<Coordinate> whitePoints, List<Graph.Node> nodes) {
        Matrix photoMatrix = new Matrix();
        indicatorImage.getSuppMatrix(photoMatrix);
        float[] matrixValues = new float[9];
        photoMatrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        PointF currentTranslate = new PointF(matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]);
        clearPath(indicatorImage);
        //disegnaPercorso(path);
        showpath = true;
        steppy = 0;
        mapDrawer.drawIndicator(x, y, i, b);
        mapDrawer.drawPath(nodes, mapImage, true);
        //disegnaTutto(whitePoints);
        //Toast.makeText(this, ""+stairPoints.size(), Toast.LENGTH_SHORT).show();
        for (Coordinate stairPoint : stairPoints) {
            if (floor)
                disegnaScala(stairPoint.getX(), stairPoint.getY(), graph2);
            else
                disegnaScala(stairPoint.getX(), stairPoint.getY(), graph1);
        }
        for (Coordinate elevator : elevatorPoints) {
            if (floor)
                disegnaAscensore(elevator.getX(), elevator.getY(), graph2);
            else
                disegnaAscensore(elevator.getX(), elevator.getY(), graph1);
        }
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
        clearPath(indicatorImage);
        disegnaPercorso(path);
        showpath = true;
        steppy = 0;
        mapDrawer.drawPath(nodes, mapImage, true);
        //disegnaTutto(whitePoints);
        //mapImage.invalidate();
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
        indicatorDrawer.resetMap();
        indicatorDrawer.drawPath(nodes, mapImage, true);
        indicatorImage.invalidate();
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

                Graph.Node node = null;
                if (floor) {
                    node = indoorNav.checkNode(graph2, pointX, pointY, whitePoints, true, stairPoints, elevatorPoints);
                } else {
                    node = indoorNav.checkNode(graph1, pointX, pointY, whitePoints, false, stairPoints, elevatorPoints);
                }
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
                    if (node.getCrowdness().equals("crowded")) {
                        sw_crowded.setChecked(true);
                    }
                    if (node.getCrowdness().equals("notCrow")) {
                        sw_crowded.setChecked(false);
                    }
                    if (node.isFixed()) {
                        sw_crowded.setClickable(false);
                        Toast.makeText(MainActivity.this, "L'affollamento e' relativo ad una persona, non può essere rimosso", Toast.LENGTH_SHORT).show();
                    }
                    else
                        sw_crowded.setClickable(true);
                    Graph.Node finalNode = node;
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
                                    if(Math.abs(coord.getX() - finalNode.getX()) < 220 && Math.abs(coord.getY() - finalNode.getY()) < 220) {
                                        unavailablePoints.remove(coord);
                                        updateAvailability(coord.getX(), coord.getY(), 110, true);
                                        loadingDismiss = false;
                                        break;
                                    }
                                }
                            } else {
                                finalNode.setAvailability("unavailable");
                                dialog.dismiss();
                                loadingDialog = new Dialog(MainActivity.this);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                                loadType.setText("Aggiungendo L'Ostacolo");
                                loadingDialog.show();
                                updateAvailability((long) finalNode.getX(), (long) finalNode.getY(), 110, false);
                                unavailablePoints.add(new Coordinate((int) finalNode.getX(), (int) finalNode.getY(), floor));
                                if(startPoint.getText().toString() != null &&
                                        endPoint.getText().toString() != null && path != null){
                                    if (floor) {
                                        path = graph2.findShortestPathAStar(startPoint.getText().toString(),
                                                endPoint.getText().toString(), stairs, available, crowd, floor);
                                    }
                                    else{
                                        path = graph1.findShortestPathAStar(startPoint.getText().toString(),
                                                endPoint.getText().toString(), stairs, available, crowd, floor);
                                    }
                                }
                                loadingDismiss = false;
                            }
                        }
                    });

                    Graph.Node finalNode1 = node;
                    sw_crowded.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (!sw_crowded.isChecked()) {
                                dialog.dismiss();
                                loadingDialog = new Dialog(MainActivity.this);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                                loadType.setText("Rimuovendo L'Affollamento");
                                loadingDialog.show();
                                for (Coordinate coord: crowdedPoints) {
                                    if(Math.abs(coord.getX() - finalNode1.getX()) < 220 && Math.abs(coord.getY() - finalNode1.getY()) < 220) {
                                        crowdedPoints.remove(coord);
                                        updateSelfCrowd(coord.getX(), coord.getY(), 110, true);
                                        loadingDismiss = false;
                                        break;
                                    }
                                }
                            } else {
                                //node.setCrowdness("crowded");
                                dialog.dismiss();
                                loadingDialog = new Dialog(MainActivity.this);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                loadingDialog.setContentView(R.layout.loading_dialog);
                                TextView loadType = loadingDialog.findViewById(R.id.txt_loading);
                                loadType.setText("Aggiungendo L'Affollamento");
                                loadingDialog.show();
                                updateSelfCrowd((long) finalNode1.getX(), (long) finalNode1.getY(), 110, false);
                                crowdedPoints.add(new Coordinate((int) finalNode1.getX(), (int) finalNode1.getY(), floor));
                                if(startPoint.getText().toString() != null &&
                                        endPoint.getText().toString() != null && path != null){
                                    path = graph.findShortestPathAStar(startPoint.getText().toString(),
                                            endPoint.getText().toString(), stairs, available, crowd, floor);
                                }
                                loadingDismiss = false;
                            }
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




