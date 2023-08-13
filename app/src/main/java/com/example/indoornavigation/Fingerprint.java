package com.example.indoornavigation;

import java.util.ArrayList;
import java.util.List;

public class Fingerprint {
    private List<ReferencePoint> referencePoints;

    public Fingerprint() {
        referencePoints = new ArrayList<>();
        // Aggiungi un punto di riferimento di default
        /*referencePoints.add(new ReferencePoint(-56, -41, -54, 2069, 2820)); //tavolo cucina
        referencePoints.add(new ReferencePoint(-57, -42, -55, 2069, 2820)); //tavolo cucina
        referencePoints.add(new ReferencePoint(-60, -43, -57, 2069, 2820)); //tavolo cucina
        referencePoints.add(new ReferencePoint(-60, -40, -58, 2069, 2820)); //tavolo cucina

        referencePoints.add(new ReferencePoint(-59, -62, -39, 2212, 1986)); //centro cameretta
        referencePoints.add(new ReferencePoint(-60, -63, -39, 2212, 1986)); //centro cameretta
        referencePoints.add(new ReferencePoint(-59, -61, -37, 2212, 1986)); //centro cameretta
        referencePoints.add(new ReferencePoint(-58, -60, -36, 2212, 1986)); //centro cameretta

        referencePoints.add(new ReferencePoint(-50, -45, -34, 2231, 2460)); //entrata circa cucina
        referencePoints.add(new ReferencePoint(-47, -52, -39, 2231, 2460)); //entrata circa cucina
        referencePoints.add(new ReferencePoint(-46, -53, -41, 2231, 2460)); //entrata circa cucina
        referencePoints.add(new ReferencePoint(-46, -50, -43, 2231, 2460)); //entrata circa cucina

        referencePoints.add(new ReferencePoint(-38, -56, -55, 1316, 2750)); //centro camera
        referencePoints.add(new ReferencePoint(-40, -53, -52, 1316, 2750)); //centro camera
        referencePoints.add(new ReferencePoint(-44, -51, -53, 1316, 2750)); //centro camera
        referencePoints.add(new ReferencePoint(-41, -50, -55, 1316, 2750)); //centro camera */

        // camera genitori
        referencePoints.add(new ReferencePoint(-22, -46, -68, 1890, 1380));
        referencePoints.add(new ReferencePoint(-31, -48, -62, 1052, 2340));
        referencePoints.add(new ReferencePoint(-38, -49, -65, 1309, 2747));
        referencePoints.add(new ReferencePoint(-40, -49, -57, 1078, 3027));
        referencePoints.add(new ReferencePoint(-35, -68, -66, 1078, 3027));
        referencePoints.add(new ReferencePoint(-39, -53, -66, 1468, 3119));
        referencePoints.add(new ReferencePoint(-41, -41, -45, 1677, 2443));
        referencePoints.add(new ReferencePoint(-32, -46, -48, 1772, 2393));
        referencePoints.add(new ReferencePoint(-38, -51, -54, 1578, 2544));

        //anticamera
        referencePoints.add(new ReferencePoint(-47, -54, -41, 2088, 2389));
        referencePoints.add(new ReferencePoint(-54, -46, -46, 2179, 2430));
        referencePoints.add(new ReferencePoint(-47, -52, -51, 2397, 2454));
        referencePoints.add(new ReferencePoint(-47, -52, -51, 2397, 2454));
        referencePoints.add(new ReferencePoint(-48, -47, -56, 2397, 2454));
        referencePoints.add(new ReferencePoint(-54, -42, -55, 2397, 2454));
    }

    public void addReferencePoint(ReferencePoint referencePoint) {
        referencePoints.add(referencePoint);
    }

    public List<ReferencePoint> getReferencePoints() {
        return referencePoints;
    }

    public static class ReferencePoint {
        private int rssi1;
        private int rssi2;
        private int rssi3;
        private int x;
        private int y;

        public ReferencePoint(int rssi1, int rssi2, int rssi3, int x, int y) {
            this.rssi1 = rssi1;
            this.rssi2 = rssi2;
            this.rssi3 = rssi3;
            this.x = x;
            this.y = y;
        }

        // Metodi getter per ottenere i valori RSSI e le coordinate
        public int getRssi1() {
            return rssi1;
        }

        public int getRssi2() {
            return rssi2;
        }

        public int getRssi3() {
            return rssi3;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }
}
