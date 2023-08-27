package com.example.indoornavigation;

public class Coordinate {
    private int x;
    private int y;
    private boolean floor;

    public Coordinate(int x, int y, boolean floor) {
        this.x = x;
        this.y = y;
        this.floor = floor;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isFloor() {
        return floor;
    }
}

