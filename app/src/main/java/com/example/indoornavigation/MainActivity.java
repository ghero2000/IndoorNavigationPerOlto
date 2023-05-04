package com.example.indoornavigation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.LinkedList;
import java.util.List;


/**
 * MainActivity è la classe principale dell'applicazione IndoorNavigationSolution.
 * Questa classe si occupa di caricare l'immagine della planimetria e di gestire il grafo
 * per la navigazione indoor, disegnando il percorso più breve tra due punti, sfruttando
 * le classi MapDrawer e Graph per la logica implementatiiva vera a propria.
 */

public class MainActivity extends AppCompatActivity {
    // Un'istanza di PhotoView che visualizza l'immagine della planimetria
    PhotoView mapImage;

    // Un'istanza di Bitmap che contiene l'immagine della planimetria
    private Bitmap mapBitmap;

    // Un'istanza di MapDrawer per disegnare percorsi sulla planimetria
    private MapDrawer mapDrawer;

    private Drawable map;
    private float mInitialDistance;
    private boolean mIsScaling;

    private TextInputEditText startPoint;

    private TextInputEditText endPoint;

    private float pointX = -1, pointY = -1;

    private Switch aSwitch;

    private String stairs = "";

    private Graph graph;

    private List<Graph.Node> path;

    private boolean touch;
    private float mLastTouchX = 0.0F;
    private float mLastTouchY = 0.0F;
    private float mScaleFactor = 1.f;

    private int stepCount = 0;

    private IndoorNavigation indoorNav;

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
        aSwitch = findViewById(R.id.switch1);

        map = getResources().getDrawable(R.drawable.planimetria);
        mapBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.planimetria);

        if (mapBitmap == null) {
            Log.e("MainActivity", "Failed to load map image. " +
                    "Ensure that the image is present in the res/drawable folder " +
                    "and its name matches the one in the code.");
            return;
        }

        mapDrawer = new MapDrawer(mapBitmap);

        indoorNav = new IndoorNavigation(mapDrawer, getApplicationContext());

        float[] touchPoint = new float[2];

        graph = new Graph(mapBitmap);
        path = null;

        graph.addNode("1", (float) 2020.6055 / 3520, (float) 1991.6936 / 4186,  "atrium");
        graph.addNode("1.1", (float) 2278.0957 / 3520, (float) 1913.4905 / 4186,  "atrium");
        graph.addNode("1.2", (float) 1769.668 / 3520, (float) 1773.3766 / 4186,  "atrium");

        graph.addNode("2", (float) 1965.1758 / 3520, (float) 2835.8684 / 4186,  "atrium");
        graph.addNode("2.1", (float) 1776.2207 / 3520, (float) 2523.056 / 4186,  "atrium");

        graph.addNode("3", (float) 866.89453 / 3520, (float) 2128.549 / 4186, "classroom");
        graph.addNode("3.1", (float) 1450.3027 / 3520, (float) 2089.4475 / 4186, "classroom");

        graph.addNode("4", (float) 827.79297 / 3520, (float) 1600.678 / 4186, "bathroom");
        graph.addNode("4.1", (float) 1029.8535 / 3520, (float) 1493.1487 / 4186, "bathroom");

        graph.addNode("5", (float) 1342.7734 / 3520, (float) 909.651 / 4186, "classroom");
        graph.addNode("5.1", (float) 1463.3008 / 3520, (float) 1209.4297 / 4186, "classroom");

        graph.addNode("6", (float) 1939.1797 / 3520, (float) 883.5833 / 4186, "classroom");
        graph.addNode("6.1", (float) 1763.1152 / 3520, (float) 1248.5312 / 4186, "classroom");

        graph.addNode("7", (float) 2046.709 / 3520, (float) 1493.1487 / 4186, "stairs");  ////////////////////////////////////////////////// modificare

        graph.addNode("8", (float) 1450.3027 / 3520, (float) 1519.2164 / 4186, "hallway");
        graph.addNode("8.1", (float) 1450.3027 / 3520, (float) 1789.669 / 4186, "hallway");
        graph.addNode("8.2", (float) 1776.2207 / 3520, (float) 1467.081 / 4186, "hallway");

        graph.addNode("9", (float) 2591.0156 / 3520, (float) 1913.4905 / 4186,  "atrium");

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

        Button drawBtn;

        drawBtn = findViewById(R.id.drawBtn);

        mapImage = findViewById(R.id.map_image);
        mapImage.setImageDrawable(map);
        mapImage.setImageBitmap(mapDrawer.getMapBitmap());
        mapImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //PhotoView mapImage2 = mapImage;

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(aSwitch.isChecked())
                    stairs = "stairs";
                else
                    stairs = "";
            }
        });

        drawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPath();
                path = graph.findShortestPath(startPoint.getText().toString(), endPoint.getText().toString(), stairs);
                disegnaPercorso(path);
                stepCount = 0;
            }
        });

        Log.d("Coordinate", "Width: "+  String.valueOf(mapBitmap.getWidth()) + "  Height: " + String.valueOf(mapBitmap.getHeight()));

        int[] testo = new int[1];
        testo[0] = 1;
        checkPoint(mapImage, touchPoint, graph, testo);


        Button stepBtn = findViewById(R.id.stepBtn);

        stepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearPath();
                indoorNav.stepNavigation (path, mapImage, stepCount);
                stepCount ++;
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

    public void checkPoint(PhotoView mapImage, float[] touchPoint, Graph graph, int[] testo){
        mapImage.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                touchPoint[0] = imageX(x, mapImage);
                touchPoint[1] = imageY(y, mapImage);
                touchPoint[0] = transformX(touchPoint[0], mapImage);
                touchPoint[1] = transformY(touchPoint[1], mapImage);
                Log.d("giova", String.valueOf(touchPoint[0]));
                Log.d("giova", String.valueOf(touchPoint[1]));
                if (touchPoint[0] != -1 && touchPoint[1] != -1) {
                    String id = "1";
                    while (graph.getNode(id) != null) {
                        Graph.Node node = graph.getNode(id);
                        if (Math.abs(touchPoint[0] - node.getX()) <= 200) {
                            if (Math.abs(touchPoint[1] - node.getY()) <= 200) {
                                //Toast.makeText(MainActivity.this, "Node"+id, Toast.LENGTH_SHORT).show();
                                if(testo[0] == 1) {
                                    startPoint.setText(id);
                                    testo[0]++;
                                    break;
                                }
                                if(testo[0] == 2) {
                                    endPoint.setText(id);
                                    testo[0]--;
                                    break;
                                }
                                break;
                            }
                        }
                        int a = Integer.parseInt(id);
                        a++;
                        id = String.valueOf(a);
                    }
                }
            }
        });
    }
    private float imageX(float touchX, PhotoView mapImage) {
        Matrix matrix = new Matrix();
        mapImage.getSuppMatrix(matrix);
        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        float scaleFactor = matrixValues[Matrix.MSCALE_X];
        float transX = matrixValues[Matrix.MTRANS_X];
        float imageX = (touchX - transX) / scaleFactor;
        return imageX;
    }

    private float imageY(float touchY, PhotoView mapImage) {
        Matrix matrix = new Matrix();
        mapImage.getSuppMatrix(matrix);
        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        float scaleFactor = matrixValues[Matrix.MSCALE_Y];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float imageY = (touchY - transY) / scaleFactor;
        return imageY;
    }

    private float transformX(float touchX, PhotoView mapImage) {
        float viewWidth = mapImage.getWidth();
        float bitmapWidth = mapBitmap.getWidth();
        float scaleFactor = Math.min((float) viewWidth / bitmapWidth, (float) mapImage.getHeight() / mapBitmap.getHeight());
        return (touchX - (viewWidth - bitmapWidth * scaleFactor) / 2) / scaleFactor;
    }

    private float transformY(float touchY, PhotoView mapImage) {
        float viewHeight = mapImage.getHeight();
        float bitmapHeight = mapBitmap.getHeight();
        float scaleFactor = Math.min((float) mapImage.getWidth() / mapBitmap.getWidth(), (float) viewHeight / bitmapHeight);
        return (touchY - (viewHeight - bitmapHeight * scaleFactor) / 2) / scaleFactor;
    }





}




