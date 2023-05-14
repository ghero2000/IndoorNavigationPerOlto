package com.example.indoornavigation;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;


/**
 * MainActivity è la classe principale dell'applicazione IndoorNavigationSolution.
 * Questa classe si occupa di caricare l'immagine della planimetria e di gestire il grafo
 * per la navigazione indoor, disegnando il percorso più breve tra due punti, sfruttando
 * le classi MapDrawer e Graph per la logica implementatiiva vera a propria.
 */

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // ATTRIBUTI PER BUSSOLA
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private ImageView compassImageView;
    private TextView degreeTextView;

    private TextView optTxt;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    // Un'istanza di PhotoView che visualizza l'immagine della planimetria
    PhotoView mapImage;

    // Un'istanza di Bitmap che contiene l'immagine della planimetria
    private Bitmap mapBitmap;

    // Un'istanza di MapDrawer per disegnare percorsi sulla planimetria
    private MapDrawer mapDrawer;

    private Drawable map;

    private TextInputEditText startPoint;

    private TextInputEditText endPoint;

    private float pointX = -1, pointY = -1;

    private Switch aSwitch; //stairs

    private Switch bSwitch; //unavailable

    private Switch cSwitch; //crowded

    private String stairs = "";

    private String available = "";

    private String crowd = "";

    private Graph graph;

    private List<Graph.Node> path;

    private int stepCount = 0;

    private IndoorNavigation indoorNav;

    private Button drawBtn;

    private TouchTransformer touchTransformer;

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

        startPoint = findViewById(R.id.starPoint);
        endPoint = findViewById(R.id.endPoint);

        map = getResources().getDrawable(R.drawable.planimetria);
        drawBtn = findViewById(R.id.drawBtn);
        mapBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.planimetria);

        touchTransformer = new TouchTransformer();

        if (mapBitmap == null) {
            Log.e("MainActivity", "Failed to load map image. " +
                    "Ensure that the image is present in the res/drawable folder " +
                    "and its name matches the one in the code.");
            return;
        }

        mapDrawer = new MapDrawer(mapBitmap);

        indoorNav = new IndoorNavigation(mapDrawer, getApplicationContext(), mapImage);

        //float[] touchPoint = new float[2];

        graph = new Graph(mapBitmap);
        path = null;

        graph.addNode("1", (float) 2020.6055 / 3520, (float) 1991.6936 / 4186,  "atrium", "available", "notCrow");
        graph.addNode("1.1", (float) 2278.0957 / 3520, (float) 1913.4905 / 4186,  "atrium", "available", "notCrow");
        graph.addNode("1.2", (float) 1769.668 / 3520, (float) 1773.3766 / 4186,  "atrium", "available", "notCrow");

        graph.addNode("2", (float) 1965.1758 / 3520, (float) 2835.8684 / 4186,  "atrium", "available", "notCrow");
        graph.addNode("2.1", (float) 1776.2207 / 3520, (float) 2523.056 / 4186,  "atrium", "available", "notCrow");

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

        graph.addNode("9", (float) 2591.0156 / 3520, (float) 1913.4905 / 4186,  "atrium", "available", "notCrow");

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

        optTxt = findViewById(R.id.btn_options);
        checkOptions();

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
                    stepCount = 0;
                }
            }
        });

        Log.d("Coordinate", "Width: "+  String.valueOf(mapBitmap.getWidth()) + "  Height: " + String.valueOf(mapBitmap.getHeight()));

        checkPoint(mapImage, graph, mapBitmap, touchTransformer);

        Button stepBtn = findViewById(R.id.stepBtn);

        stepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPath(true);
                indoorNav.stepNavigation (path, mapImage, stepCount);
                stepCount ++;
            }
        });

        //onCreate per bussola
        compassImageView = findViewById(R.id.compass_image_view);
        degreeTextView = findViewById(R.id.degree_text_view);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //---------- fine bussola
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
                }
                else aSwitch.setChecked(false);
                if (available == "available") {
                    bSwitch.setChecked(true);
                }
                else bSwitch.setChecked(false);
                if (crowd == "crowded") {
                    cSwitch.setChecked(true);
                }
                else cSwitch.setChecked(false);

                aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(aSwitch.isChecked())
                            stairs = "stairs";
                        else
                            stairs = "";
                    }
                });

                bSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(bSwitch.isChecked())
                            available = "unavailable";
                        else
                            available = "";
                    }
                });

                cSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(cSwitch.isChecked())
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

    public void clearPath(){
        mapDrawer.resetMap(); // Aggiungi questa riga per ripristinare la mappa nel MapDrawer
        mapImage.setImageBitmap(mapDrawer.getMapBitmap()); // Imposta la nuova mappa ripristinata
        mapImage.invalidate(); // Forza il ridisegno della PhotoView
    }

    public void clearPath(boolean cosa){
        mapImage.invalidate(); // Forza il ridisegno della PhotoView
    }

    public void checkPoint(PhotoView mapImage, Graph graph, Bitmap mapBitmap, TouchTransformer touchTransformer){
        mapImage.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                float pointX = touchTransformer.transformX(x, mapImage, mapBitmap);
                float pointY = touchTransformer.transformY(y, mapImage, mapBitmap);
                Graph.Node node = indoorNav.checkNode(graph, pointX, pointY, startPoint, endPoint);
                final Dialog dialog = new Dialog(MainActivity.this);

                //  Imposta il layout del tuo dialog personalizzato
                dialog.setContentView(R.layout.custom_dialog);

                TextView node_name = dialog.findViewById(R.id.node_name);
                TextView node_id = dialog.findViewById(R.id.node_id);
                TextView node_type = dialog.findViewById(R.id.node_type);

                if(node != null) {
                    node_name.setText("Node: " + node.getId());
                    node_id.setText(node.getId());
                    node_type.setText(node.getRoomType());

                    Button btn_start = dialog.findViewById(R.id.start_btn);
                    Button btn_end = dialog.findViewById(R.id.end_btn);

                    Switch sw_crowded = dialog.findViewById(R.id.sw_crowded);
                    Switch sw_available = dialog.findViewById(R.id.sw_available);

                    btn_start.setOnClickListener(new View.OnClickListener() {
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

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
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
            mapImage.setRotation(-degrees);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}




