package net.sf.openrocket.gui.print;

import net.sf.openrocket.gui.print.visitor.CenteringRingStrategy;
import net.sf.openrocket.rocketcomponent.CenteringRing;
import net.sf.openrocket.rocketcomponent.ClusterConfiguration;
import net.sf.openrocket.rocketcomponent.InnerTube;
import net.sf.openrocket.util.ArrayList;
import net.sf.openrocket.util.Coordinate;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class creates a renderable centering ring.  It depends only on AWT/Swing and can be called from other actors
 * (like iText handlers) to render the centering ring on different graphics contexts.
 */
public class PrintableCenteringRing extends AbstractPrintable<CenteringRing> {
    /**
     * If the component to be drawn is a centering ring, save a reference to it.
     */
    private CenteringRing target;

    /**
     * The line length of the cross hairs.
     */
    private final int lineLength = 10;

    /**
     * A set of the inner 'holes'.  At least one, but will have many if clustered.
     */
    private Set<CenteringRingStrategy.Dimension> innerCenterPoints = new HashSet<CenteringRingStrategy.Dimension>();

    /**
     * Construct a simple, non-clustered, printable centering ring.
     *
     * @param theRing       the component to print
     * @param theMotorMount the motor mount if clustered, else null
     */
    private PrintableCenteringRing(CenteringRing theRing, InnerTube theMotorMount) {
        super(false, theRing);
        if (theMotorMount == null || theMotorMount.getClusterConfiguration().equals(ClusterConfiguration.SINGLE)) {
            //Single motor.
            innerCenterPoints.add(new CenteringRingStrategy.Dimension((float) PrintUnit.METERS.toPoints(target.getOuterRadius()),
                    (float) PrintUnit.METERS.toPoints(target.getOuterRadius())));
        }
        else {
            List<Coordinate> coords = theMotorMount.getClusterPoints();
            populateCenterPoints(coords);
        }
    }

    /**
     * Constructor for a clustered centering ring.
     *
     * @param theRing        the centering ring component
     * @param theMotorMounts a list of the motor mount tubes that are physically supported by the centering ring
     */
    private PrintableCenteringRing(CenteringRing theRing, List<InnerTube> theMotorMounts) {
        super(false, theRing);
        List<Coordinate> points = new ArrayList<Coordinate>();
        //Transform the radial positions of the tubes.
        for (InnerTube it : theMotorMounts) {
            double y = it.getRadialShiftY();
            double z = it.getRadialShiftZ();
            Coordinate coordinate = new Coordinate(0, y, z);
            points.add(coordinate);
        }
        populateCenterPoints(points);
    }

    /**
     * Factory method to create a printable centering ring.
     *
     * @param theRing        the component to print
     * @param theMotorMounts the motor mount if clustered, else null
     */
    public static PrintableCenteringRing create(CenteringRing theRing, List<InnerTube> theMotorMounts) {
        if (theMotorMounts == null) {
            return new PrintableCenteringRing(theRing, (InnerTube) null);
        }
        else if (theMotorMounts.size() <= 1) {
            return new PrintableCenteringRing(theRing, theMotorMounts.isEmpty() ? null : theMotorMounts.get(0));
        }
        else {
            return new PrintableCenteringRing(theRing, theMotorMounts);
        }
    }

    /**
     * Initialize the set of center points for each motor mount tube, based on the tube coordinates.
     *
     * @param theCoords the list of tube coordinates; each coordinate is in the OR units (meters) and must be
     *                  transformed to the printing (points) coordinate system
     */
    private void populateCenterPoints(final List<Coordinate> theCoords) {
        float radius = (float) PrintUnit.METERS.toPoints(target.getOuterRadius());
        for (Coordinate coordinate : theCoords) {
            innerCenterPoints.add(new CenteringRingStrategy.Dimension((float) PrintUnit.METERS.toPoints
                    (coordinate.y) + radius,
                    (float) PrintUnit.METERS.toPoints(coordinate.z) + radius));
        }
    }

    /**
     * @param component the centering ring component
     */
    @Override
    protected void init(final CenteringRing component) {

        target = component;

        double radius = target.getOuterRadius();
        setSize((int) PrintUnit.METERS.toPoints(2 * radius),
                (int) PrintUnit.METERS.toPoints(2 * radius));
    }

    /**
     * Draw a centering ring.
     *
     * @param g2 the graphics context
     */
    @Override
    protected void draw(Graphics2D g2) {
        double radius = PrintUnit.METERS.toPoints(target.getOuterRadius());

        Color original = g2.getBackground();
        Shape outerCircle = new Ellipse2D.Double(0, 0, radius * 2, radius * 2);
        g2.setColor(Color.lightGray);
        g2.fill(outerCircle);
        g2.setColor(Color.black);
        g2.draw(outerCircle);

        for (CenteringRingStrategy.Dimension next : innerCenterPoints) {
            drawInnerCircle(g2, next.getWidth(), next.getHeight());
        }
        g2.setColor(original);
    }

    /**
     * Draw one inner circle, representing the motor mount tube, with cross hairs in the center.
     *
     * @param g2         the graphics context
     * @param theCenterX the center x in points
     * @param theCenterY the center y in points
     */
    private void drawInnerCircle(final Graphics2D g2, final double theCenterX, final double theCenterY) {
        double innerRadius = PrintUnit.METERS.toPoints(target.getInnerRadius());
        Shape innerCircle = new Ellipse2D.Double(theCenterX - innerRadius, theCenterY - innerRadius, innerRadius * 2, innerRadius * 2);
        g2.setColor(Color.white);
        g2.fill(innerCircle);
        g2.setColor(Color.black);
        g2.draw(innerCircle);

        drawCross(g2, (int) theCenterX, (int) theCenterY, lineLength, lineLength);
    }

    /**
     * Draw the center cross-hair.
     *
     * @param g      the graphics context
     * @param x      the x coordinate of the center point
     * @param y      the y coordinate of the center point
     * @param width  the width in pixels of the horizontal hair
     * @param height the width in pixels of the vertical hair
     */
    private void drawCross(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.black);
        ((Graphics2D) g).setStroke(thinStroke);
        g.drawLine(x - width / 2, y, x + width / 2, y);
        g.drawLine(x, y - height / 2, x, y + height / 2);
    }

}