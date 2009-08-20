package jd.captcha.easy;

import java.awt.Color;
import java.awt.Point;
import java.io.Serializable;

import jd.captcha.pixelgrid.Captcha;
import jd.nutils.Colors;

public class CPoint extends Point implements Serializable, Cloneable {

    public final static byte LAB_DIFFERENCE = 1;
    public final static byte RGB_DIFFERENCE1 = 2;
    public final static byte RGB_DIFFERENCE2 = 3;
    public final static byte HUE_DIFFERENCE = 4;
    public final static byte SATURATION_DIFFERENCE = 5;
    public final static byte BRIGHTNESS_DIFFERENCE = 6;
    public final static byte RED_DIFFERENCE = 7;
    public final static byte GREEN_DIFFERENCE = 8;
    public final static byte BLUE_DIFFERENCE = 9;

    private static final long serialVersionUID = 333616481245029882L;
    private int color, distance;
    /**
     * Fordergrund oder Hintergrund Buchstaben oder Hintergrund
     */
    private boolean foreground = true;

    private byte colorDifferenceMode = LAB_DIFFERENCE;

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     */
    public CPoint() {
    }

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     * 
     * @param x
     * @param y
     * @param distance
     * @param captcha
     */
    public CPoint(int x, int y, int distance, Captcha captcha) {
        this(x, y, distance, captcha.getPixelValue(x, y));
    }

    /**
     * Beim CPoint wird der Point um Farbeigenschaften erweitert und stellt
     * verschiedene möglichkeiten zur verfügung um Farbunterschiede zu berechnen
     * 
     * @param x
     * @param y
     * @param distance
     * @param color
     */
    public CPoint(int x, int y, int distance, int color) {
        super(x, y);
        this.color = color;
        this.distance = distance;
    }

    /**
     * Farbe
     * 
     * @return
     */
    public int getColor() {
        return color;
    }

    /**
     * Farbe
     * 
     * @param color
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Farbmodus
     * 
     * @param colorDistanceMode
     *            CPoint.LAB_DIFFERENCE CPoint.RGB_DIFFERENCE1 ...
     */
    public byte getColorDistanceMode() {
        return colorDifferenceMode;
    }

    /**
     * Farbmodus
     * 
     * @param colorDistanceMode
     *            CPoint.LAB_DIFFERENCE CPoint.RGB_DIFFERENCE1 ...
     */
    public void setColorDistanceMode(byte colorDistanceMode) {
        this.colorDifferenceMode = colorDistanceMode;
    }

    /**
     * handelt es sich um einen Fordergrund / Buchstaben oder um Hintergrund
     * 
     * @return false wenn Hintergrund
     */
    public boolean isForeground() {
        return foreground;
    }

    /**
     * @param Wenn
     *            Fordergrund / Buchstaben dann true beim Hintergrund false
     */
    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

    /**
     * Erlaubter Farbunterschied
     * 
     * @return
     */
    public int getDistance() {
        return distance;
    }

    /**
     * gibt anhand von colorDifferenceMode unterschied zur übergebenen Farbe aus
     * 
     * @param color
     * @return
     */
    public double getColorDifference(int color) {
        double dst = 0;

        if (color == this.color) return dst;
        switch (colorDifferenceMode) {
        case LAB_DIFFERENCE:
            dst = Colors.getColorDifference(color, this.color);
            break;
        case RGB_DIFFERENCE1:
            dst = Colors.getRGBColorDifference1(color, this.color);
            break;
        case RGB_DIFFERENCE2:
            dst = Colors.getRGBColorDifference2(color, this.color);
            break;
        case HUE_DIFFERENCE:
            dst = Colors.getHueColorDifference360(color, this.color);
            break;
        case SATURATION_DIFFERENCE:
            dst = Colors.getSaturationColorDifference(color, this.color);
            break;
        case BRIGHTNESS_DIFFERENCE:
            dst = Colors.getBrightnessColorDifference(color, this.color);
            break;
        case RED_DIFFERENCE:
            dst = Math.abs(new Color(color).getRed() - new Color(this.color).getRed());
            break;
        case GREEN_DIFFERENCE:
            dst = Math.abs(new Color(color).getGreen() - new Color(this.color).getGreen());
            break;
        case BLUE_DIFFERENCE:
            dst = Math.abs(new Color(color).getBlue() - new Color(this.color).getBlue());
            break;
        default:
            dst = Colors.getColorDifference(color, this.color);
            break;
        }
        return dst;
    }

    /**
     * 
     * @param Erlaubter
     *            Farbunterschied
     */
    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public Object clone() {
        return new CPoint(x, y, distance, color);
    }

    /**
     * prüft ob die farbe und
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || (obj != null && obj instanceof CPoint && ((CPoint) obj).color == color);
    }
}
