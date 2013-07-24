package common;

public class Rectangle {
  int x,y,w,h,xm,ym;
  public Rectangle(int _x, int _y, int _w, int _h) {
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    if(w<0) { // fix width and height
      w = 0 - w;
      x -= w;
    }
    if(h<0) {
      h = 0 - h;
      y -= h;
    }
    xm = x+w; // calculate maxes to speed up optimizations
    ym = y+h;
  }
  public int getX() { return x; }
  public int getY() { return y; }
  public int getW() { return w; }
  public int getH() { return h; }
  public boolean insideRect(int px, int py) {
    return ((px>=x) && (px<xm) && (py>=y) && (py<ym));
  }
  public static boolean insideRect(int px, int py, int x, int y, int xw, int yw) {
    return ((px>=x) && (px<(x+xw)) && (py>=y) && (py<(y+yw)));
  }
}
