package com.example.indoornavigation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.github.chrisbanes.photoview.OnMatrixChangedListener;


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
        linePaint.setColor(Color.BLUE);
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

    /**
     * Effettua uno zoom sulla view in modo tale che il centro del percorso (1/2 X E Y DEI PUNTI DI PARTENZA E ARRIVO)
     * sia centrato nella view.
     *
     * @param mapView La view da zoomare
     * @param nodes   Lista di nodi che rappresentano il percorso
     */
    public void zoomOnPath(PhotoView mapView, List<Graph.Node> nodes) {
        int lengthNodes = nodes.size();

        Graph.Node start = nodes.get(0);
        Graph.Node end = nodes.get(lengthNodes - 1);

        float startX = start.getX() * mapBitmap.getWidth();
        float startY = start.getY() * mapBitmap.getHeight();

        float endX = end.getX() * mapBitmap.getWidth();
        float endY = end.getY() * mapBitmap.getHeight();

        int centerX;
        int centerY;

        if (startX <= endX) {
            centerX = (int) (((Math.abs(startX - endX)) / 2) + startX);
        } else {
            centerX = (int) (((Math.abs(startX - endX)) / 2) + endX);
        }

        if (startY <= endY) {
            centerY = (int) (((Math.abs(startY - endY)) / 2) + startY);
        } else {
            centerY = (int) (((Math.abs(startY - endY)) / 2) + endY);
        }

        float zoomScale = (mapBitmap.getWidth() * mapBitmap.getHeight()) / (Math.abs(startX - endX) * Math.abs(startY - endY));

        boolean animate = true;
       /* try {
            mapView.setScale(zoomScale, animate);
        } catch (Exception IllegalArgumentException) {
            mapView.setScale(mapView.getMaximumScale(), animate);
        }
        */
        // Calcola le nuove coordinate centrali in base alle coordinate in pixel
        /*float newCenterX = centerX / (float) mapBitmap.getWidth();
        float newCenterY = centerY / (float) mapBitmap.getHeight();
        */

        int distanceX = (int)((- mapView.getWidth() / 2) + start.getX() * mapView.getWidth()) ;
        int distanceY = (int)((- mapView.getHeight() / 2) + start.getY() * mapView.getHeight());

        // Sposta la vista all'interno della PhotoView
        mapView.scrollTo((int) distanceX, (int) distanceY);

    }










    /*public void drowLine(PhotoView mapImage){

        mapImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                float startX = -1;
                float startY = -1;
                float endX = -1;
                float endY = -1;
                float imgX = imageX(event.getX(),  mapImage);
                float imgY = imageY(event.getY(), mapImage);

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (startX == -1 && startY == -1) {
                        // Imposta il punto di partenza
                        startX = imgX;
                        startY = imgY;

                    } else if (endX == -1 && endY == -1) {
                        // Imposta il punto di arrivo
                        endX = imgX;
                        endY = imgY;

                        // Disegna la linea tra i due punti
                        mapCanvas.drawLine(startX, startY, endX, endY, linePaint);
                        mapImage.invalidate();

                        Log.d("Coordinate", String.valueOf(startX) + "   " + String.valueOf(startY));
                        Log.d("Coordinate", String.valueOf(endX) + "   " + String.valueOf(endY));

                        // Reimposta i valori di partenza e arrivo per il prossimo tracciamento
                        startX = -1;
                        startY = -1;
                        endX = -1;
                        endY = -1;
                    }
                }
                return true;
            }
        });
    }

    private float imageX(float touchX, PhotoView mapImage) {
        float viewWidth = mapImage.getWidth();
        float bitmapWidth = mapBitmap.getWidth();
        float scaleFactor = Math.min((float) viewWidth / bitmapWidth, (float) mapImage.getHeight() / mapBitmap.getHeight());
        return (touchX - (viewWidth - bitmapWidth * scaleFactor) / 2) / scaleFactor;
    }

    private float imageY(float touchY, PhotoView mapImage) {
        float viewHeight = mapImage.getHeight();
        float bitmapHeight = mapBitmap.getHeight();
        float scaleFactor = Math.min((float) mapImage.getWidth() / mapBitmap.getWidth(), (float) viewHeight / bitmapHeight);
        return (touchY - (viewHeight - bitmapHeight * scaleFactor) / 2) / scaleFactor;
    }*/

}

