package com.gdx.cellular.elements.liquid;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.gdx.cellular.CellularAutomaton;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.ElementType;
import com.gdx.cellular.elements.EmptyCell;
import com.gdx.cellular.elements.gas.Gas;
import com.gdx.cellular.elements.solid.Solid;
import com.gdx.cellular.elements.solid.movable.MovableSolid;
import com.gdx.cellular.particles.Particle;

public abstract class Liquid extends Element {

    public int density;
    public int dispersionRate;

    public Liquid(int x, int y, boolean isPixel) {
        super(x, y, isPixel);
    }

//    public void draw(ShapeRenderer sr) {
//        sr.setColor(color);
//        sr.rect(pixelX, pixelY, CellularAutomaton.pixelSizeModifier, CellularAutomaton.pixelSizeModifier);
//    }

    public void step(CellularMatrix matrix) {
        if (stepped.get(0) == CellularAutomaton.stepped.get(0)) return;
        stepped.flip(0);

        if (matrix.useChunks && !matrix.shouldElementInChunkStep(this)) {
            return;
        }

        vel.add(CellularAutomaton.gravity);
        if (isFreeFalling) vel.x *= .8;

        int yModifier = vel.y < 0 ? -1 : 1;
        int xModifier = vel.x < 0 ? -1 : 1;
        int velYDeltaTime = (int) (Math.abs(vel.y) * Gdx.graphics.getDeltaTime());
        int velXDeltaTime = (int) (Math.abs(vel.x) * Gdx.graphics.getDeltaTime());

        boolean xDiffIsLarger = Math.abs(velXDeltaTime) > Math.abs(velYDeltaTime);

        int upperBound = Math.max(Math.abs(velXDeltaTime), Math.abs(velYDeltaTime));
        int min = Math.min(Math.abs(velXDeltaTime), Math.abs(velYDeltaTime));
        int freq = (min == 0 || upperBound == 0) ? 0 : (upperBound / min);

        int smallerCount = 0;
        Vector3 formerLocation = new Vector3(matrixX, matrixY, 0);
        Vector3 lastValidLocation = new Vector3(matrixX, matrixY, 0);
        for (int i = 1; i <= upperBound; i++) {
            if (freq != 0 && i % freq == 0 && min >= smallerCount) {
                smallerCount += 1;
            }

            int yIncrease, xIncrease;
            if (xDiffIsLarger) {
                xIncrease = i;
                yIncrease = smallerCount;
            } else {
                yIncrease = i;
                xIncrease = smallerCount;
            }

            int modifiedMatrixY = matrixY + (yIncrease * yModifier);
            int modifiedMatrixX = matrixX + (xIncrease * xModifier);
            if (matrix.isWithinBounds(modifiedMatrixX, modifiedMatrixY)) {
                Element neighbor = matrix.get(modifiedMatrixX, modifiedMatrixY);
                if (neighbor == this) continue;
                boolean stopped = actOnNeighboringElement(neighbor, matrix, i == upperBound, i == 1, lastValidLocation, 0);
                if (stopped) {
                    break;
                }
                lastValidLocation.x = modifiedMatrixX;
                lastValidLocation.y = modifiedMatrixY;

            } else {
                matrix.setElementAtIndex(matrixX, matrixY, ElementType.EMPTYCELL.createElementByPixel(pixelX, pixelY));
                return;
            }
        }
        applyHeatToNeighborsIfIgnited(matrix);
        modifyColor();
        spawnSparkIfIgnited(matrix);
        checkLifeSpan(matrix);
        takeEffectsDamage(matrix);
        if (matrix.useChunks) {
            if (movedPreviousFrame || isIgnited || formerLocation.x != matrixX || formerLocation.y != matrixY) {
                matrix.reportToChunkActive(this);
            }
        }
    }

    @Override
    protected boolean actOnNeighboringElement(Element neighbor, CellularMatrix matrix, boolean isFinal, boolean isFirst, Vector3 lastValidLocation, int depth) {
        boolean acted = actOnOther(neighbor, matrix);
        if (acted) return true;
        if (neighbor instanceof EmptyCell || neighbor instanceof Particle) {
            setAdjacentNeighborsFreeFalling(matrix, depth, lastValidLocation);
            if (isFinal) {
                isFreeFalling = true;
                swapPositions(matrix, neighbor);
            } else {
                return false;
            }
        } else if (neighbor instanceof Liquid) {
            Liquid liquidNeighbor = (Liquid) neighbor;
            if (compareDensities(liquidNeighbor)) {
                swapLiquidForDensities(matrix, liquidNeighbor, lastValidLocation);
                return false;
            }
            if (depth > 0) {
                return true;
            }
            if (isFinal) {
                moveToLastValid(matrix, lastValidLocation);
                return true;
            }
            if (isFreeFalling) {
                float absY = Math.max(Math.abs(vel.y) / 31, 105);
                vel.x = vel.x < 0 ? -absY : absY;
            }
            Vector3 normalizedVel = vel.cpy().nor();
            int additionalX = getAdditional(normalizedVel.x);
            int additionalY = getAdditional(normalizedVel.y);

            int distance = additionalX * (Math.random() > 0.5 ? dispersionRate + 2 : dispersionRate - 1);

            Element diagonalNeighbor = matrix.get(matrixX + additionalX, matrixY + additionalY);
            if (isFirst) {
                vel.y = getAverageVelOrGravity(vel.y, neighbor.vel.y);
            } else {
                vel.y = -124;
            }

            neighbor.vel.y = vel.y;
            vel.x *= frictionFactor;
            if (diagonalNeighbor != null) {
                boolean stoppedDiagonally = iterateToAdditional(matrix, diagonalNeighbor.matrixX, diagonalNeighbor.matrixY, distance);
                if (!stoppedDiagonally) {
                    isFreeFalling = true;
                    return true;
                }
            }

            Element adjacentNeighbor = matrix.get(matrixX + additionalX, matrixY);
            if (adjacentNeighbor != null && adjacentNeighbor != diagonalNeighbor) {
                boolean stoppedAdjacently = iterateToAdditional(matrix, adjacentNeighbor.matrixX, adjacentNeighbor.matrixY, distance);
                if (stoppedAdjacently) vel.x *= -1;
                if (!stoppedAdjacently) {
//                    isFreeFalling = false;
                    return true;
                }
            }

//            isFreeFalling = false;

            moveToLastValid(matrix, lastValidLocation);
            return true;
        } else if (neighbor instanceof Solid) {
            if (depth > 0) {
                return true;
            }
            if (isFinal) {
                moveToLastValid(matrix, lastValidLocation);
                return true;
            }
            if (isFreeFalling) {
                float absY = Math.max(Math.abs(vel.y) / 31, 105);
                vel.x = vel.x < 0 ? -absY : absY;
            }
            Vector3 normalizedVel = vel.cpy().nor();
            int additionalX = getAdditional(normalizedVel.x);
            int additionalY = getAdditional(normalizedVel.y);

            int distance = additionalX * (Math.random() > 0.5 ? dispersionRate + 2 : dispersionRate - 1);

            Element diagonalNeighbor = matrix.get(matrixX + additionalX, matrixY + additionalY);
            if (isFirst) {
                vel.y = getAverageVelOrGravity(vel.y, neighbor.vel.y);
            } else {
                vel.y = -124;
            }

            neighbor.vel.y = vel.y;
            vel.x *= frictionFactor;
            if (diagonalNeighbor != null) {
                boolean stoppedDiagonally = iterateToAdditional(matrix, diagonalNeighbor.matrixX, diagonalNeighbor.matrixY, distance);
                if (!stoppedDiagonally) {
                    isFreeFalling = true;
                    return true;
                }
            }

            Element adjacentNeighbor = matrix.get(matrixX + additionalX, matrixY);
            if (adjacentNeighbor != null) {
                boolean stoppedAdjacently = iterateToAdditional(matrix, adjacentNeighbor.matrixX, adjacentNeighbor.matrixY, distance);
                if (stoppedAdjacently) vel.x *= -1;
                if (!stoppedAdjacently) {
//                    isFreeFalling = false;
                    return true;
                }
            }

//            isFreeFalling = false;

            moveToLastValid(matrix, lastValidLocation);
            return true;
        } else if (neighbor instanceof Gas) {
            if (isFinal) {
                moveToLastValidAndSwap(matrix, neighbor, lastValidLocation);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean iterateToAdditional(CellularMatrix matrix, int startingX, int startingY, int distance) {
        int distanceModifier = distance > 0 ? 1 : -1;
        Vector3 lastValidLocation = new Vector3(matrixX, matrixY, 0);
        for (int i = 0; i <= Math.abs(distance); i++) {
            Element neighbor = matrix.get(startingX + i * distanceModifier, startingY);
            if (neighbor == null) {
                return true;
            }
            boolean acted = actOnOther(neighbor, matrix);
            if (acted) return false;
            boolean isFirst = i == 0;
            boolean isFinal = i == Math.abs(distance);
            if (neighbor == null) continue;
            if (neighbor instanceof EmptyCell || neighbor instanceof Particle) {
                if (isFinal) {
                    swapPositions(matrix, neighbor);
                    return false;
                }
                lastValidLocation.x = startingX + i * distanceModifier;
                lastValidLocation.y = startingY;
                continue;
            } else if (neighbor instanceof Liquid) {
                Liquid liquidNeighbor = (Liquid) neighbor;
                if (compareDensities(liquidNeighbor)) {
                    swapLiquidForDensities(matrix, liquidNeighbor, lastValidLocation);
                    return false;
                }
                continue;
            } else if (neighbor instanceof Solid) {
                if (isFirst) {
                    return true;
                }
                moveToLastValid(matrix, lastValidLocation);
                return false;
            }
        }
        return true;
    }

    private void swapLiquidForDensities(CellularMatrix matrix, Liquid neighbor, Vector3 lastValidLocation) {
        vel.y = -62;
        moveToLastValidAndSwap(matrix, neighbor, lastValidLocation);
    }

    private boolean compareDensities(Liquid neighbor) {
        return (density > neighbor.density && neighbor.matrixY <= matrixY); // ||  (density < neighbor.density && neighbor.matrixY >= matrixY);
    }

    private void setAdjacentNeighborsFreeFalling(CellularMatrix matrix, int depth, Vector3 lastValidLocation) {
        if (depth > 0) return;

        Element adjacentNeighbor1 = matrix.get(lastValidLocation.x + 1, lastValidLocation.y);
        if (adjacentNeighbor1 instanceof Solid) {
            boolean wasSet = setElementFreeFalling(adjacentNeighbor1);
            if (wasSet) {
                matrix.reportToChunkActive(adjacentNeighbor1);
            }
        }

        Element adjacentNeighbor2 = matrix.get(lastValidLocation.x - 1, lastValidLocation.y);
        if (adjacentNeighbor2 instanceof Solid) {
            boolean wasSet = setElementFreeFalling(adjacentNeighbor2);
            if (wasSet) {
                matrix.reportToChunkActive(adjacentNeighbor2);
            }
        }
    }

    private boolean setElementFreeFalling(Element element) {
        element.isFreeFalling = Math.random() > element.inertialResistance || element.isFreeFalling;
        return element.isFreeFalling;
    }



    private int getAdditional(float val) {
        if (val < -.1f) {
            return (int) Math.floor(val);
        } else if (val > .1f) {
            return (int) Math.ceil(val);
        } else {
            return 0;
        }
    }

    private float getAverageVelOrGravity(float vel, float otherVel) {
        if (otherVel > -125f) {
            return -124f;
        }
        float avg = (vel + otherVel) / 2;
        if (avg > 0) {
            return avg;
        } else {
            return Math.min(avg, -124f);
        }
    }

}
