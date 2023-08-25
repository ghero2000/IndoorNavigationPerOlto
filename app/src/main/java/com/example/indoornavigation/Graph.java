package com.example.indoornavigation;

import android.graphics.Bitmap;
import android.util.Log;

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
    public void addNode(String id, float x, float y, String roomType, String availability, String crowdness) {
        nodes.put(id, new Node(id, x, y, roomType,
                availability, crowdness));
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

    public List<Node> findShortestPath(String start, String end, String roomType, String available, String crowd) {

        Set<String> unvisited = new HashSet<>(nodes.keySet());          // praticamente è il l'heap
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();

        for (String node : nodes.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);

        while (!unvisited.isEmpty()) {                                                       // finchè non è vuoto
            String current = findClosestNode(distances, unvisited, roomType, available, crowd);         // estraizone del nodo con peso minore ("il più vicino")
            try {
                if (current.equals(end)) {
                    break;
                }
            }catch(NullPointerException e) {
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

    private String findClosestNode(Map<String, Integer> distances, Set<String> unvisited, String roomType, String available, String crowd) {
        String closestNode = null;
        int minDistance = Integer.MAX_VALUE;

        for (String node : unvisited) {
            int distance = distances.get(node);
            if (distance < minDistance && !nodes.get(node).getRoomType().equals(roomType)
                && !nodes.get(node).getAvailability().equals(available)
                && !nodes.get(node).getCrowdness().equals(crowd)) {
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

    /*
    public List<Node> findShortestPath(String start, String end, String roomType, String available, String crowd,
                                       int numAnts, int numIterations, double evaporationRate,
                                       double alpha, double beta, double q) {

        // Inizializza i feromoni sugli archi
        initializePheromones();

        List<Node> shortestPath = new ArrayList<>();
        double shortestPathLength = Double.POSITIVE_INFINITY;

        for (int iteration = 0; iteration < numIterations; iteration++) {
            List<List<Node>> antPaths = new ArrayList<>();
            double[] antPathLengths = new double[numAnts];

            // Costruisci soluzioni delle formiche
            for (int antIndex = 0; antIndex < numAnts; antIndex++) {
                List<Node> antPath = constructAntPath(start, end, roomType, available, crowd, alpha, beta);
                antPaths.add(antPath);
                antPathLengths[antIndex] = calculatePathLength(antPath);
            }

            // Trova la migliore soluzione tra le formiche
            int bestAntIndex = findBestAnt(antPathLengths);
            if (antPathLengths[bestAntIndex] < shortestPathLength) {
                shortestPath = antPaths.get(bestAntIndex);
                shortestPathLength = antPathLengths[bestAntIndex];
            }

            // Aggiorna i feromoni sugli archi
            updatePheromones(antPaths, antPathLengths, q, evaporationRate);
        }

        return shortestPath;
    }
    private void initializePheromones() {
        for (Node node : nodes.values()) {
            for (Edge edge : node.getEdges()) {
                edge.setPheromone(1.0); // Inizialmente tutti i feromoni sono impostati a 0.0
            }
        }
    }

    // Costruisci il percorso di una formica usando regole di scelta basate su feromoni
    private List<Node> constructAntPath(String start, String end, String roomType, String available, String crowd,
                                        double alpha, double beta) {
        List<Node> antPath = new ArrayList<>();
        Node current = getNode(start);

        while (!current.getId().equals(end)) {
            List<Edge> validEdges = current.getEdges().stream()
                    .filter(edge -> isValidEdge(edge, roomType, available, crowd))
                    .collect(Collectors.toList());

            double[] probabilities = calculateProbabilities(current, validEdges, alpha, beta);
            Edge chosenEdge = chooseEdgeWithProbability(validEdges, probabilities);

            antPath.add(current);
            current = chosenEdge.getDestination();
        }

        antPath.add(current); // Aggiungi l'ultimo nodo (destinazione)
        return antPath;
    }

    // Verifica se un arco è valido per la formica
    private boolean isValidEdge(Edge edge, String roomType, String available, String crowd) {
        Node destination = edge.getDestination();

        return !destination.getRoomType().equals(roomType) ||
                !destination.getAvailability().equals(available) ||
                !destination.getCrowdness().equals(crowd);
    }

    // Calcola le probabilità di scelta degli archi basate su feromoni ed euristica migliorata
    private double[] calculateProbabilities(Node current, List<Edge> edges, double alpha, double beta) {
        double total = 0;
        double[] probabilities = new double[edges.size()];

        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            double pheromone = edge.getPheromone();
            double heuristic = 1.0 / (edge.getWeight() * edge.getWeight()); // Euristica migliorata

            probabilities[i] = Math.pow(pheromone, alpha) * Math.pow(heuristic, beta);
            total += probabilities[i];
        }

        // Normalizza le probabilità
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= total;
        }

        return probabilities;
    }


    // Scegli un arco in base alle probabilità calcolate
    private Edge chooseEdgeWithProbability(List<Edge> edges, double[] probabilities) {
        double randomValue = Math.random();
        double cumulativeProbability = 0;

        for (int i = 0; i < edges.size(); i++) {
            cumulativeProbability += probabilities[i];
            if (randomValue <= cumulativeProbability) {
                return edges.get(i);
            }
        }

        // Se per qualche motivo non è stata scelta alcuna
        return edges.get(edges.size() - 1);
    }

    // Aggiorna i feromoni sugli archi in base ai risultati delle formiche
    private void updatePheromones(List<List<Node>> antPaths, double[] antPathLengths,
                                  double q, double evaporationRate) {
        for (Node node : nodes.values()) {
            for (Edge edge : node.getEdges()) {
                double deltaPheromone = 0;

                for (int antIndex = 0; antIndex < antPaths.size(); antIndex++) {
                    List<Node> antPath = antPaths.get(antIndex);
                    if (antPath.contains(node) && antPath.contains(edge.getDestination())) {
                        deltaPheromone += q / antPathLengths[antIndex];
                    }
                }

                edge.setPheromone((1 - evaporationRate) * edge.getPheromone() + deltaPheromone);
            }
        }
    }

    // Trova l'indice della migliore soluzione tra le formiche
    private int findBestAnt(double[] antPathLengths) {
        int bestAntIndex = 0;
        double shortestPathLength = antPathLengths[0];

        for (int antIndex = 1; antIndex < antPathLengths.length; antIndex++) {
            if (antPathLengths[antIndex] < shortestPathLength) {
                shortestPathLength = antPathLengths[antIndex];
                bestAntIndex = antIndex;
            }
        }

        return bestAntIndex;
    }


    // Calcola la lunghezza totale del percorso
    private double calculatePathLength(List<Node> path) {
        double length = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);

            for (Edge edge : current.getEdges()) {
                if (edge.getDestination().equals(next)) {
                    length += edge.getWeight();
                    break;
                }
            }
        }

        return length;
    } */

    //A STAR
    public List<Node> findShortestPathAStar(String start, String end, String roomType, String available, String crowd) {
        Set<String> openSet = new HashSet<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();

        openSet.add(start);
        gScore.put(start, 0.0);

        if (end.equals("Secondo Piano")) {
            String nearestStaircaseNode = "";
            // Find the nearest node with roomType "staircase" from the start
            if(!roomType.equals("stairs")) {
                nearestStaircaseNode = findNearestStaircaseNode(start, crowd);
            }
            else {
                nearestStaircaseNode = findNearestElevatorNode(start, crowd);
            }
            end = nearestStaircaseNode; // Update end to the nearest staircase node
        }

        fScore.put(start, calculateHeuristic(start, end, roomType, available, crowd));

        while (!openSet.isEmpty()) {
            String current = findLowestFScoreNode(openSet, fScore);

            if (current.equals(end)) {
                return reconstructPath(cameFrom, current);
            }

            openSet.remove(current);
            closedSet.add(current);

            for (Edge edge : nodes.get(current).getEdges()) {
                String neighborId = edge.getDestination().getId();

                if (closedSet.contains(neighborId)) {
                    continue;
                }

                double tentativeGScore = gScore.get(current) + edge.getWeight();

                if (!openSet.contains(neighborId) || tentativeGScore < gScore.get(neighborId)) {
                    cameFrom.put(neighborId, current);
                    gScore.put(neighborId, tentativeGScore);
                    fScore.put(neighborId, tentativeGScore + calculateHeuristic(neighborId, end, roomType, available, crowd));

                    if (!openSet.contains(neighborId)) {
                        openSet.add(neighborId);
                    }
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    private String findLowestFScoreNode(Set<String> openSet, Map<String, Double> fScore) {
        String lowestNode = null;
        double lowestScore = Double.POSITIVE_INFINITY;

        for (String node : openSet) {
            double score = fScore.getOrDefault(node, Double.POSITIVE_INFINITY);
            if (score < lowestScore) {
                lowestScore = score;
                lowestNode = node;
            }
        }

        return lowestNode;
    }

    private List<Node> reconstructPath(Map<String, String> cameFrom, String current) {
        LinkedList<Node> path = new LinkedList<>();
        while (current != null) {
            path.addFirst(nodes.get(current));
            current = cameFrom.get(current);
        }
        return path;
    }
    /*
    private double calculateHeuristic(String nodeId, String end) {
        Node currentNode = nodes.get(nodeId);
        Node endNode = nodes.get(end);

        double xDistance = Math.abs(currentNode.getX() - endNode.getX());
        double yDistance = Math.abs(currentNode.getY() - endNode.getY());

        return Math.sqrt(xDistance * xDistance + yDistance * yDistance);
    } */

    private double calculateHeuristic(String nodeId, String end, String roomType, String available, String crowd) {
        //Log.d("crowd", ""+crowd);
        Node currentNode = nodes.get(nodeId);
        Node endNode = nodes.get(end);

        double xDistance = Math.abs(currentNode.getX() - endNode.getX());
        double yDistance = Math.abs(currentNode.getY() - endNode.getY());

        boolean roomTypeSatisfied = currentNode.getRoomType().equals(roomType);
        boolean availableSatisfied = currentNode.getAvailability().equals(available);
        boolean crowdSatisfied = currentNode.getCrowdness().equals(crowd);
        //Log.d("crowd", ""+crowdSatisfied);

        double penalty = 0.0;
        if (roomTypeSatisfied || availableSatisfied || crowdSatisfied) {
            penalty = 14000.0; // Apply a penalty if constraints are not satisfied
            Log.d("crowd", ""+penalty);
        }

        return xDistance + yDistance + penalty;
    }

    private String findNearestStaircaseNode(String start, String crowd) {
        double shortestDistance = Double.POSITIVE_INFINITY;
        double shortestDistanceWithCrowdness = Double.POSITIVE_INFINITY;
        String nearestStaircaseNode = null;
        String nearestStaircaseNodeWithCrowdness = null;

        for (String nodeId : nodes.keySet()) {
            Node node = nodes.get(nodeId);
            if (node.getRoomType().equals("stairs") || node.getRoomType().equals("elevator")) {
                double distance = calculateDistance(start, nodeId);
                double distanceWithCrowdness = calculateDistance(start, nodeId);
                if (distance < shortestDistance && !node.getCrowdness().equals("crowded")) {
                    shortestDistance = distance;
                    nearestStaircaseNode = nodeId;
                }
                if (distanceWithCrowdness < shortestDistanceWithCrowdness && node.getCrowdness().equals("crowded")) {
                    shortestDistanceWithCrowdness = distanceWithCrowdness;
                    nearestStaircaseNodeWithCrowdness = nodeId;
                }
            }
        }

        if (shortestDistance == Double.POSITIVE_INFINITY) {
            nearestStaircaseNode = nearestStaircaseNodeWithCrowdness;
        }
        if (shortestDistanceWithCrowdness <= shortestDistance && crowd.equals("")) {
            nearestStaircaseNode = nearestStaircaseNodeWithCrowdness;
        }

        return nearestStaircaseNode;
    }

    private String findNearestElevatorNode(String start, String crowd) {
        double shortestDistance = Double.POSITIVE_INFINITY;
        double shortestDistanceWithCrowdness = Double.POSITIVE_INFINITY;
        String nearestStaircaseNode = null;
        String nearestStaircaseNodeWithCrowdness = null;

        for (String nodeId : nodes.keySet()) {
            Node node = nodes.get(nodeId);
            if (node.getRoomType().equals("elevator")) {
                double distance = calculateDistance(start, nodeId);
                double distanceWithCrowdness = calculateDistance(start, nodeId);
                if (distance < shortestDistance && !node.getCrowdness().equals("crowded")) {
                    shortestDistance = distance;
                    nearestStaircaseNode = nodeId;
                }
                if (distanceWithCrowdness < shortestDistanceWithCrowdness && node.getCrowdness().equals("crowded")) {
                    shortestDistanceWithCrowdness = distance;
                    nearestStaircaseNodeWithCrowdness = nodeId;
                }
            }
        }

        if (shortestDistance == Double.POSITIVE_INFINITY) {
            nearestStaircaseNode = nearestStaircaseNodeWithCrowdness;
        }

        if (shortestDistanceWithCrowdness <= shortestDistance && crowd.equals("")) {
            nearestStaircaseNode = nearestStaircaseNodeWithCrowdness;
        }

        return nearestStaircaseNode;
    }

    private double calculateDistance(String node1Id, String node2Id) {
        Node node1 = nodes.get(node1Id);
        Node node2 = nodes.get(node2Id);

        double xDistance = Math.abs(node1.getX() - node2.getX());
        double yDistance = Math.abs(node1.getY() - node2.getY());

        return Math.sqrt(xDistance * xDistance + yDistance * yDistance);
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
        private String availability;
        private String crowdness;
        private List<Edge> edges;
        private boolean fixed;

        public Node(String id, float x, float y, String roomType, String availability, String crowdness) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.availability = availability;
            this.crowdness = crowdness;
            this.roomType = roomType;
            this.fixed = false;

            /*if(roomType == "atrium" || roomType == "elevator" || roomType == "stairs" || roomType == "classroom" || roomType == "bathroom" || roomType == "hallway"){
                this.roomType = roomType;
            }else{
                Log.d("mlgione", roomType+"");
                throw new IllegalArgumentException("Invalid room type");
            }*/
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

        public String getAvailability() {
            return availability;
        }

        public void setAvailability(String availability) {
            this.availability = availability;
        }

        public String getCrowdness() {
            return crowdness;
        }

        public void setCrowdness(String crowdness) {
            this.crowdness = crowdness;
        }

        public void setFixed(boolean b) {
            this.fixed = b;
        }

        public boolean isFixed() {
            return fixed;
        }
    }


    /**
     * Classe interna Edge che rappresenta un arco nel grafo.
     * Contiene un nodo destinazione e un peso associato all'arco.
     */
    public static class Edge {
        private Node destination;
        private int weight;

        private double pheromone;

        public double getPheromone() {
            return pheromone;
        }

        public void setPheromone(double pheromone) {
            this.pheromone = pheromone;
        }

        public Edge(Node destination, int weight) {
            this.destination = destination;
            this.weight = weight;
            this.pheromone = 0.1;
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
