import java.lang.Math;

public class Point
{
  public double x;
  public double y;
  
  public Point(double xp, double yp){
    x = xp;
    y = yp;
  }
  public double getDistTo(Point p) {
    return Math.sqrt((p.x -x)*(p.x -x) + (p.y -y)*(p.y-y));
  }
}