package org.waitlight.codememo.simple;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * <a href="https://rosettacode.org/wiki/Ray-casting_algorithm">...</a>
 * <a href="http://philliplemons.com/posts/ray-casting-algorithm">...</a>
 * <p>
 * 大量多边形使用射线法求解，可以使用<a href="https://www.cnblogs.com/LBSer/p/4471742.html"> R </a> 树优化效率
 */
public class RayCasting {

    public static boolean contains(Point p, Polygon polygon) {
        boolean inside = false;
        Point[] points = polygon.points;
        int len = points.length;
        for (int i = 0; i < len; i++) {
            if (intersects(points[i], points[(i + 1) % len], p)) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean intersects(Point a, Point b, Point p) {
        if (a.y > b.y) return intersects(b, a, p);
        if (p.y == a.y || p.y == b.y) p.y += 0.0001;
        if (p.y > b.y || p.y < a.y || p.x >= max(a.x, b.x)) return false;
        if (p.x < min(a.x, b.x)) return true;

        double red = (p.y - a.y) / (p.x - a.x);
        double blue = (b.y - a.y) / (b.x - a.x);
        return red >= blue;
    }
}
