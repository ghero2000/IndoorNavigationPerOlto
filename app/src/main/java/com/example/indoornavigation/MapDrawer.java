package com.example.indoornavigation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.widget.Toast;

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
        if (b && radius > 5) {
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.YELLOW);
            circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            circlePaint.setAlpha(90);
            circlePaint.setAntiAlias(true);
            mapCanvas.drawCircle(x4, y4, (float) radius, circlePaint);
        }
        if (b && radius <=5) {
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.RED);
            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setAlpha(90);
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

    public void drawIndicator(long x, long y, int i, boolean b1) {
        Paint paint = new Paint();
        if (b1)
            paint.setColor(Color.YELLOW);
        else
            paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(128);
        //paint.setAlpha(128); // Imposta l'opacità trasparente.
        float radius = i; // Imposta il raggio del cerchio.
        paint.setAntiAlias(true);
        mapCanvas.drawCircle(x, y, radius, paint);
    }

    public void drawElevator(int x, int y) {
        int centerX = x;
        int centerY = y;
        int elevatorWidth = 120;
        int elevatorHeight = 160;
        //int ladderWidth = 40;
        int ladderHeight = 80;
        int stepCount = 3;
        int stepHeight = ladderHeight / stepCount;
        int doorWidth = 40;
        int doorHeight = 140;

        Paint paint = new Paint();

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);

        // Draw elevator
        int elevatorY = centerY - stepCount * stepHeight;
        mapCanvas.drawRect(centerX - elevatorWidth / 2, elevatorY,
                centerX + elevatorWidth / 2, elevatorY + elevatorHeight, paint);

        paint.setColor(Color.GRAY);

        // Draw elevator doors
        int doorX1 = centerX - elevatorWidth / 4 - doorWidth / 2;
        int doorX2 = centerX + elevatorWidth / 4 - doorWidth / 2;
        mapCanvas.drawRect(doorX1, 15+elevatorY, doorX1 + doorWidth, elevatorY + doorHeight, paint);
        mapCanvas.drawRect(doorX2, 15+elevatorY, doorX2 + doorWidth, elevatorY + doorHeight, paint);
    }

    public void drawStair(int x, int y, Context applicationContext) {
        int centerX = x;
        int centerY = y;
        int ladderWidth = 120;
        int ladderHeight = 300;
        int stepCount = 10;
        int stepHeight = ladderHeight / stepCount;

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);

        // Draw ladder sides
        mapCanvas.drawLine(centerX - ladderWidth / 2, centerY,
                centerX - ladderWidth / 2, centerY - ladderHeight, paint);
        mapCanvas.drawLine(centerX + ladderWidth / 2, centerY,
                centerX + ladderWidth / 2, centerY - ladderHeight, paint);

        //paint.setColor(Color.GRAY);

        // Draw ladder steps
        for (int i = 0; i < stepCount; i++) {
            int stepY = centerY - i * stepHeight;
            mapCanvas.drawLine(centerX - ladderWidth / 2, stepY,
                    centerX + ladderWidth / 2, stepY, paint);
        }
    }
}

