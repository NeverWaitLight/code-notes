package org.waitlight.codememo.simple;

import org.junit.jupiter.api.Test;

class RayCastingTest {
    public static final double[][] squarePoints = {{0d, 0d}, {20d, 0d}, {20d, 20d}, {0d, 20d}};
    public static final Polygon square = new Polygon(squarePoints);

    public static final double[][] squareHolePoints = {{0, 0}, {20, 0}, {20, 20}, {0, 20}, {5, 5}, {15, 5}, {15, 15}, {5, 15}};
    public static final Polygon squareHole = new Polygon(squareHolePoints);

    public static final double[][] strangePoints = {{0, 0}, {5, 5}, {0, 20}, {5, 15}, {15, 15}, {20, 20}, {20, 0}};
    public static final Polygon strange = new Polygon(strangePoints);

    public static final double[][] hexagonPoints = {{6, 0}, {14, 0}, {20, 10}, {14, 20}, {6, 20}, {0, 10}};
    public static final Polygon hexagon = new Polygon(hexagonPoints);

    public static final Polygon[] polygons = {square, squareHole, strange, hexagon};

    public static final Point[] testPoints = {
            new Point(10, 10),
            new Point(10, 16),
            new Point(-20, 10),
            new Point(0, 10),
            new Point(20, 10),
            new Point(16, 10),
            new Point(20, 20)
    };

    @Test
    void contains() {
        for (Point testPoint : testPoints) {
            for (Polygon polygon : polygons) {
                System.out.println(RayCasting.contains(testPoint, polygon));
            }
        }
    }

}