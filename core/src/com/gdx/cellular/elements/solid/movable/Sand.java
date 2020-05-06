package com.gdx.cellular.elements.solid.movable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.elements.solid.movable.MovableSolid;

public class Sand extends MovableSolid {

    public Sand(int x, int y, boolean isPixel) {
        super(x, y, isPixel);
        vel = new Vector3(Math.random() > 0.5 ? -1 : 1, -124f,0f);
        frictionFactor = 0.9f;
        inertialResistance = .1f;
        mass = 150;
        color = Color.YELLOW;
        defaultColor = Color.YELLOW;
    }

    @Override
    public boolean receiveHeat(int heat) {
        return false;
    }

}
