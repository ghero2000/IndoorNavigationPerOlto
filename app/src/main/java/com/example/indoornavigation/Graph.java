package com.example.indoornavigation;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * La classe Graph rappresenta un grafo non orientato pesato.
 * Fornisce metodi per aggiungere nodi e archi al grafo, e per trovare il percorso più
 * breve tra due nodi utilizzando l'algoritmo di Dijkstra.
 */
public class Graph {

    // Mappa dei nodi contenuti nel grafo
    private Map<String, Node> nodes;

    private Bitmap mapBitmap;

    /**
     * Costruttore della classe Graph.
     * Inizializza la mappa dei nodi.
     */
    public Graph(Bitmap mapBitmap) {
        nodes = new HashMap<>();
        this.mapBitmap = mapBitmap;
    }

    /**
     * Aggiunge un nodo al grafo.
     *
     * @param id Identificativo univoco del nodo
     * @param x Coordinata x del nodo
     * @param y Coordinata y del nodo
     */
    public void addNode(String id, float x, float y, String roomType) {
        nodes.put(id, new Node(id, x * mapBitmap.getWidth(), y * mapBitmap.getHeight(), roomType));
    }

    /**
     * Aggiunge un arco tra due nodi nel grafo.
     *
     * @param source Identificativo del nodo sorgente
     * @param destination Identificativo del nodo destinazione
     * @param weight Peso dell'arco
     */
    public void addEdge(String source, String destination, int weight) {
        nodes.get(source).addEdge(new Edge(nodes.get(destination), weight));
        nodes.get(destination).addEdge(new Edge(nodes.get(source), weight));
    }

    /**
     * Restituisce il nodo corrispondente all'ID specificato.
     *
     * @param id Identificativo del nodo da restituire
     * @return Il nodo corrispondente all'ID specificato
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * Trova il percorso più breve tra due nodi utilizzando l'algoritmo di Dijkstra.
     *
     * @param start Identificativo del nodo di partenza
     * @param end Identificativo del nodo di arrivo
     * @return Una lista di nodi che rappresenta il percorso più breve tra i nodi di partenza e arrivo
     */
    public List<Node> findShortestPath(String start, String end, String roomType) {

        Set<String> unvisited = new HashSet<>(nodes.keySet());          // praticamente è il l'heap
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();

        for (String node : nodes.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);

        while (!unvisited.isEmpty()) {                                                       // finchè non è vuoto
            String current = findClosestNode(distances, unvisited, roomType);         // estraizone del nodo con peso minore ("il più vicino")
            if (current.equals(end)) {
                break;
            }

            int currentDistance = distances.get(current);
            for (Edge edge : nodes.get(current).getEdges()) {               // per ogni arco che parte da current applichiamo la procedura di relax
                String neighborId = edge.getDestination().getId();
                if (unvisited.contains(neighborId)) {
                    int newDistance = currentDistance + edge.getWeight();
                    if (newDistance < distances.get(neighborId)) {
                        distances.put(neighborId, newDistance);
                        predecessors.put(neighborId, current);
                    }
                }
            }
            unvisited.remove(current);
        }

        return reconstructPath(predecessors, start, end);
    }


    /*&& !nodes.get(node).getRoomType().equals(roomType) */

    private String findClosestNode(Map<String, Integer> distances, Set<String> unvisited, String roomType) {
        String closestNode = null;
        int minDistance = Integer.MAX_VALUE;

        for (String node : unvisited) {
            int distance = distances.get(node);
            if (distance < minDistance && !nodes.get(node).getRoomType().equals(roomType) ) {
                minDistance = distance;
                closestNode = node;
            }
        }

        return closestNode;
    }

    private List<Node> reconstructPath(Map<String, String> predecessors, String start, String end) {
        LinkedList<Node> path = new LinkedList<>();

        String currentNode = end;
        while (currentNode != null && !currentNode.equals(start)) {
            path.addFirst(nodes.get(currentNode));
            currentNode = predecessors.get(currentNode);
        }

        if (currentNode != null && currentNode.equals(start)) {
            path.addFirst(nodes.get(start));
        } else {
            path.clear();
        }

        return path;
    }

    /**
     * Classe interna Node che rappresenta un nodo nel grafo.
     * Contiene un identificativo, le coordinate x e y, e una lista di archi adiacenti.
     */
    public static class Node {
        private String id;
        private float x;
        private float y;
        private String roomType;
        private List<Edge> edges;

        public Node(String id, float x, float y, String roomType) {
            this.id = id;
            this.x = x;
            this.y = y;
            if(roomType == "classroom" || roomType == "elevator" || roomType == "stairs" || roomType ==  "atrium" || roomType == "bathroom" || roomType == "hallway"){
                this.roomType = roomType;
            }else{
                throw new IllegalArgumentException("Invalid room type");
            }
            edges = new ArrayList<>();
        }

        // Aggiungi i metodi getter per x e y

        /**
         * Restituisce la coordinata x del nodo.
         *
         * @return La coordinata x del nodo
         */
        public float getX() {
            return x;
        }

        /**
         * Restituisce la coordinata y del nodo.
         *
         * @return La coordinata y del nodo
         */
        public float getY() {
            return y;
        }

        public void addEdge(Edge edge) {
            edges.add(edge);
        }

        public String getId() {
            return id;
        }

        public String getRoomType(){
            return roomType;
        }

        /**
         * Restituisce la lista di archi adiacenti al nodo.
         *
         * @return La lista di archi adiacenti al nodo
         */
        public List<Edge> getEdges() {
            return edges;
        }
    }


    /**
     * Classe interna Edge che rappresenta un arco nel grafo.
     * Contiene un nodo destinazione e un peso associato all'arco.
     */
    public static class Edge {
        private Node destination;
        private int weight;

        public Edge(Node destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }

        /**
         * Restituisce il nodo destinazione dell'arco.
         *
         * @return Il nodo destinazione dell'arco
         */
        public Node getDestination() {
            return destination;
        }

        /**
         * Restituisce il peso dell'arco.
         *
         * @return Il peso dell'arco
         */
        public int getWeight() {
            return weight;
        }
    }
}
