package com.example.indoornavigation;

import android.content.Context;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.nio.MappedByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class IndoorNavigation {
    private MapDrawer mapDrawer;

    private Context context;

    public IndoorNavigation(MapDrawer mapDrawer, Context context) {
        this.mapDrawer = mapDrawer;
        this.context = context;
    }

    public void stepNavigation(List<Graph.Node> path, PhotoView mapView, int count){

        if(path == null || path.size() == 0){

            Toast.makeText(context  , "Warning: Select a possible path", Toast.LENGTH_SHORT).show();

        } else if(count + 1   >= path.size()){

            Toast.makeText(context  , "you arrived!!", Toast.LENGTH_SHORT).show();

        }else {

            Graph.Node node2 = path.get(count + 1);

            Graph.Node node = path.get(count);

            List<Graph.Node> edge = new LinkedList<>();
            edge.add(node);
            edge.add(node2);

            mapDrawer.drawStep(path, mapView, edge);

        }

    }

}
