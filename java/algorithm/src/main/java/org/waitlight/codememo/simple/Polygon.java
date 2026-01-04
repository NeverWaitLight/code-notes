package org.waitlight.codememo.simple;

public class Polygon implements Shape {
    public Point[] points;

    public Polygon(double[][] points) {
        Point[] p = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            double[] pnt = points[i];
            p[i] = new Point(pnt[0], pnt[1]);
        }
        this.points = p;
    }
}
