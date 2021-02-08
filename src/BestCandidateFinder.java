import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Class to generate besst candidate circles
 * @author Umesh P Narendran
 * @since Feb 2021
 */
public class BestCandidateFinder {

    public static class Circle {
        private double x;
        private double y;
        private double radius;
        public Circle(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
        public String toString() {
            return String.format("(%6.1f, %6.1f) : %7.2f", this.x, this.y, this.radius);
        }
        public double getX() { return this.x;}
        public double getY() { return this.y;}
        public double getR() { return this.radius;}
    }

    /** Debug levels. 0 = no debug.*/
    int debugLevel = 1;

    /** Maximum number of circles drawn. */
    private final int maxCircles = 2500;

    /** Minimum X co-ordinate for the center. */
    private final double xMin;

    /** Maximum X co-ordinate for the center for the center. */
    private final int xMax;

    /** Minimum Y co-ordinate for the center. */
    private final int yMin;

    /** Maximum Y co-ordinate for the center. */
    private final int yMax;

    /** Number of circles to be generated to select the best from that.  */
    private double k;

    /** Minimum radius for the circles. */
    private final double minR;

    /** Maximum radius for the circles. */
    private final double maxR;

    /** Number of circles made with the same radius. */
    private int nMaxCirclesWithSameRadius;

    /** Some times, a circle of this radius cannot be put.  In this case, after trying these many times, we reduce radius. */
    private int numberOfMaxTrialsBeforeReducingRadius = 5000;

    /** Current count of circles with a particular radius. */
    private int nCirclesForThisRadius = 0;

    private int nCirclesDrawn;

    /** Current radius. */
    private double r;

    /** Current list of already generated circles. */
    List<Circle> circles;

    /** Random variables to generate X and Y. */
    Random xRand = new Random();
    Random yRand = new Random();

    /** Pre-generated circle to be returned on the next call to nextCircle(). */
    Circle newCircle;

    /**
     *
     * @param xMax Maximum X value for the center.
     * @param yMax Maximum Y Value for the center.
     * @param minR Minimum radius of circles.
     * @param maxR Maximum radius of the circles.
     * @param k Initial number of circles generated to choose one among them.
     * @param nMaxCirclesWithSameRadius Number of circles chosen with same radius.
     */
    public BestCandidateFinder(int xMax, int yMax, int minR, int maxR, int k, int nMaxCirclesWithSameRadius) {
        this.xMin = 0;
        this.yMin = 0;
        this.xMax = xMax;
        this.yMax = yMax;
        this.minR = minR;
        this.maxR = maxR;
        this.nMaxCirclesWithSameRadius = nMaxCirclesWithSameRadius;
        this.k = k;

        // Start generation.
        this.nCirclesDrawn = 0;
        this.circles = new ArrayList<>();
        this.r = this.maxR;
        if (debugLevel > 1) {
            System.out.printf("k = %f, r = %f\n", this.k, this.r);
        }
    }

    /**
     * Returns the next circle that doesn't cross any existing ones and is farthest from them,
     * among k tried circles.
     * @return next random circle.  Returns {@code null}, if (a) 2500 circles have been returned OR
     * (b) no further circles can be drawn.
     */
    public Circle nextCircle() {

        // If the maximum number of circles for the current radius is over, reduce radius and increase k.
        if (nCirclesForThisRadius > this.nMaxCirclesWithSameRadius) {
            setNextK();
            setNextR();
            if (debugLevel > 1) {
                System.out.printf("Next radius: k = %f, r = %f\n", this.k, this.r);
            }
            nCirclesForThisRadius = 0;
        }

        for (;;) {
            ++this.nCirclesDrawn;
            if (this.nCirclesDrawn > this.maxCircles) {
                System.out.printf("%d circles drawn\n", this.maxCircles);
                return null;
            }

            this.newCircle = generateNextCircle();
            if (this.newCircle == null) {
                // No more circles can be drawn at this level.
                if (adjustParameters()) {
                    // Try with new parameters.
                    continue;
                }

                // No more circles can be drawn in any level.
                return null;
            }

            // We have a circle in this level.

            addNewCircleToList();
            ++nCirclesForThisRadius;
            return this.newCircle;
        }
    }

    private void addNewCircleToList() {
        if (debugLevel > 0) {
            System.out.printf("(%4d) Added circle:  %s.\n", this.nCirclesDrawn, this.newCircle);
        }
        this.circles.add(this.newCircle);
        ++this.nCirclesForThisRadius;
    }

    private boolean adjustParameters() {
        setNextK();    // Increase k.
        setNextR();    // Reduce r.
        if (debugLevel > 1) {
            System.out.printf("k = %f, r = %f\n", this.k, this.r);
        }
        this.nCirclesForThisRadius = 0;
        return (this.r >= this.minR);
    }

    /** Generates the next radius and store that in this.r */
    private boolean setNextR() {
        // return this.minR + (maxRadius - this.minR) * rRand.nextDouble();
        this.r *= 0.98;
        return this.r >= this.minR;
    }

    /** Generates the next k and store that in this.k */
    private void setNextK() {
        this.k *= 1.01;
    }

    /**
     * Generates a circle that doesn't cross any existing ones and is farther from them.
     */
    Circle generateNextCircle() {
        double largestDistance = 0.0;
        Circle bestCircle = null;

        // Generate k circles and choose the best.
        int validCircleCount = 0;        // Current count of valid circles checked.
        int nTrialsForThisCircle = 0;    // Total trials (valid and invalid) tried for this circle.
        while (validCircleCount < this.k) {
            ++nTrialsForThisCircle;
            if (nTrialsForThisCircle > this.numberOfMaxTrialsBeforeReducingRadius) {
                return null;
            }
            Circle c = generateOneCircle();

            // Find its distance from the nearest existing circle.
            double distance = findSmallestDistance(c);

            if (debugLevel > 2) {
                System.out.printf("    Circle (%d:%d) %s, smallest distance = %f\n", validCircleCount,
                        nTrialsForThisCircle, distance);
            }

            // Generate another circle if the current one crosses any other.  We don't increment validCircleCount.
            if (distance < 0) {
                continue;
            }

            // Select this circle as a potential selection if this is the more distant than all the previous.
            if (distance > largestDistance) {
                largestDistance = distance;
                bestCircle = c;
                if (debugLevel > 2) {
                    System.out.println("      So far farthest.");
                }
            }

            // We got a non-intersecting circle.  Try another one.
            nTrialsForThisCircle = 0;
            ++validCircleCount;
        }

        // Now, bestCircle is the circle farthest from all other circles.  Return it.
        if (debugLevel > 2) {
            System.out.printf("    Got a circle %s\n", bestCircle);
        }
        return bestCircle;
    }

    /**
     * Returns the distance of a circle to the nearest existing circle.
     * @param c The circle
     * @return Distance too the nearest circle.  If it crosses any of other circles, returns -1.0.
     */
    private double findSmallestDistance(Circle c) {
        double smallestDistance = Double.MAX_VALUE;
        for (Circle old : circles) {
            double d = distanceBetween(c, old);

            // We need not check others if one circle crosses.
            if (d < 0.0) {
                return -1.0;
            }

            if (d < smallestDistance) {
                smallestDistance = d;
            }
        }
        return smallestDistance;
    }

    private Circle generateOneCircle() {

        for (;;) {
            // Generate x between minX and maxX.
            double x = generateX();

            // Generate y between minY and maxY.
            double y = generateY();

            // Create circle with the values.
            Circle c = new Circle(x, y, this.r);
            return c;
        }
    }

    private double generateX() {
        // Generate a random number between xMin and xMax;
        return xMin + (xMax - xMin) * xRand.nextDouble();
    }

    private double generateY() {
        // Generate a random number between yMin and yMax;
        return yMin + (yMax - yMin) * yRand.nextDouble();
    }

    private double distanceBetween(Circle c1, Circle c2) {
        double xDiff = c1.x - c2.x;
        double yDiff = c1.y - c2.y;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff) - (c1.radius + c2.radius);
    }

    @SuppressWarnings("unused")
    private double minimumDistanceToEdges(int x, int y) {
        double xDiff = Math.min(x - this.xMin, this.xMax - x);
        double yDiff = Math.min(y - this.yMin, this.yMax - y);
        return Math.min(xDiff, yDiff);
    }

    @SuppressWarnings("unused")
    private int nextPoisson(double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            ++k;
            p *= xRand.nextDouble();
        } while (p > l);
        return k - 1;
    }

    public static void main(String[] args) {
        int width = 320;
        int height = 540;
        int minimumRadius = 1;
        int maximumRadius = 30;
        int k = 30;
        int N = 20;
        BestCandidateFinder f = new BestCandidateFinder(width, height, minimumRadius, maximumRadius, k, N);
        System.out.printf("width = %d, height = %d, min. radius = %d, max. radius = %d\n", width, height, minimumRadius, maximumRadius);
        // testPoisson(f);
        testAlgorithm(f);
    }

    @SuppressWarnings("unused")
    private static void testPoisson(BestCandidateFinder f) {
        int lambda = 10;
        for (int i = 0; i < 20; ++i) {
            System.out.println(f.nextPoisson(lambda));
        }
    }

    private static void testAlgorithm(BestCandidateFinder f) {
        for (int i = 0; i < 30; ++i) {
            System.out.println(f.generateNextCircle());
        }
    }
}
