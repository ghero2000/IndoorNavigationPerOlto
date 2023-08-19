package com.example.indoornavigation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.github.chrisbanes.photoview.PhotoView;


import java.util.List;


/**
 * La classe MapDrawer gestisce il disegno dei percorsi sulla planimetria.
 * Fornisce metodi per disegnare e cancellare percorsi sulla mappa, utilizzando un oggetto Canvas
 * e un oggetto Paint per personalizzare lo stile delle linee tracciate.
 */
public class MapDrawer {

    private Bitmap originalBitmap;
    // Un'istanza di Bitmap che contiene l'immagine della planimetria
    private Bitmap mapBitmap;

    // Un'istanza di Canvas per disegnare sulla planimetria
    private Canvas mapCanvas;

    // Un'istanza di Paint per configurare lo stile delle linee tracciate
    private Paint linePaint;

    private Paint grayLinePaint;

    /**
     * Costruttore della classe MapDrawer.
     * Inizializza le variabili e configura lo stile delle linee tracciate.
     *
     * @param mapBitmap Bitmap dell'immagine della planimetria
     */
    public MapDrawer(Bitmap mapBitmap) {

        this.originalBitmap = mapBitmap;
        this.mapBitmap = mapBitmap.copy(Bitmap.Config.ARGB_8888, true);
        this.mapCanvas = new Canvas(this.mapBitmap);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#8C4444"));
        linePaint.setStrokeWidth(25);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        grayLinePaint = new Paint();
        grayLinePaint.setColor(Color.rgb(128, 128, 128));
        grayLinePaint.setStrokeWidth(25);
        grayLinePaint.setStyle(Paint.Style.STROKE);
        grayLinePaint.setStrokeJoin(Paint.Join.ROUND);

    }


    /**
     * Restituisce l'immagine della planimetria come un oggetto Bitmap.
     *
     * @return Un oggetto Bitmap che rappresenta l'immagine della planimetria
     */
    public Bitmap getMapBitmap() {
        return mapBitmap;
    }

    /**
     * Disegna un percorso sulla planimetria utilizzando una lista di nodi.
     * Ogni nodo nella lista contiene le coordinate x e y del punto sulla planimetria.
     *
     * @param nodes Lista di nodi che rappresentano il percorso da disegnare
     */
    public void drawPath(List<Graph.Node> nodes, PhotoView mapView, Boolean lineType ) {
        if (nodes == null || nodes.size() < 2) {
            return;
        }

        Path path = new Path();
        Graph.Node firstNode = nodes.get(0);
        path.moveTo(firstNode.getX(), firstNode.getY());

        for (int i = 1; i < nodes.size(); i++) {
            Graph.Node currentNode = nodes.get(i);
            path.lineTo(currentNode.getX(), currentNode.getY());
        }

        if(lineType){
            mapCanvas.drawPath(path, linePaint);
        }else{
            mapCanvas.drawPath(path, grayLinePaint);
        }

        //zoomOnPath(mapView, nodes);
    }

    public void drawStep(List<Graph.Node> nodes, PhotoView mapView, List<Graph.Node> step) {
        drawPath(nodes, mapView, false);
        if (step == null || step.size() < 2) {
            return;
        }

        Path path = new Path();
        Graph.Node firstNode = step.get(0);
        path.moveTo(firstNode.getX(), firstNode.getY());

        for (int i = 1; i < step.size(); i++) {
            Graph.Node currentNode = step.get(i);
            path.lineTo(currentNode.getX(), currentNode.getY());
        }

        mapCanvas.drawPath(path, linePaint);
        //zoomOnPath(mapView, nodes);
    }




    public void resetMap() {
        // Crea una copia dell'immagine originale
        mapBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        mapCanvas = new Canvas(mapBitmap);
    }

    public void drawIndicator(float x, float y) {
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.GREEN);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);
        mapCanvas.drawCircle(x,y,40,circlePaint);
    }

    public void drawIndicator(float x4, float y4, boolean b, double radius) {
        if (b) {
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setAntiAlias(true);
            mapCanvas.drawCircle(x4, y4, (float) radius, circlePaint);
        }
        else {
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.BLUE);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setAntiAlias(true);
            mapCanvas.drawCircle(x4, y4, (float) radius, circlePaint);
        }
    }
}

