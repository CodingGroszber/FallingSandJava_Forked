package com.gdx.cellular.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularAutomaton;

import static com.gdx.cellular.input.MouseMode.*;

public class Cursor {

    public MouseMode mode = SPAWN;
    public int brushSize;
    public int pixelX;
    public int pixelY;

    public Cursor(MouseMode mode, int brushSize, int pixelX, int pixelY) {
        this.mode = mode;
        this.brushSize = brushSize;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
    }

    public void draw(ShapeRenderer sr) {
        switch(mode) {
            case SPAWN:
                sr.begin();
                sr.set(ShapeRenderer.ShapeType.Line);
                sr.setColor(Color.RED);
                sr.circle(this.pixelX, this.pixelY, this.brushSize - 2);
                sr.end();
                break;

            default:
        }

    }

    public void update(MouseMode mode, int brushSize, int pixelX, int pixelY, OrthographicCamera camera) {
        this.mode = mode;
        this.brushSize = brushSize;
        this.pixelX = pixelX/CellularAutomaton.pixelSizeModifier * CellularAutomaton.pixelSizeModifier + 1;
        this.pixelY = pixelY/CellularAutomaton.pixelSizeModifier * CellularAutomaton.pixelSizeModifier + 1;
    }
}
