package map;

import java.io.Serializable;

public strictfp class AffineTransform implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static AffineTransform ID = new AffineTransform(1, 0, 0, 0, 1, 0);
  
  public static AffineTransform chain(AffineTransform... transforms) {
    AffineTransform result = ID;
    for (AffineTransform transform : transforms)
      result = transform.after(result);
    return result;
  }
  
  public static AffineTransform getTranslation(double x, double y) {
    return new AffineTransform(1, 0, x, 0, 1, y);
  }
  
  public static AffineTransform getLinearTransform(double mxx, double mxy, double myx, double myy) {
    return new AffineTransform(mxx, mxy, 0, myx, myy, 0);
  }
  
  public static AffineTransform getRotation(double phi) {
    double cos = Math.cos(phi);
    double sin = Math.sin(phi);
    
    return getLinearTransform(cos, -sin, sin, cos);
  }
  
  public static AffineTransform getScaling(double r) {
    return getLinearTransform(r, 0, 0, r);
  }
  
  private double mxx, mxy, mxz;
  private double myx, myy, myz;
  
  public AffineTransform(double mxx, double mxy, double mxz, double myx, double myy, double myz) {
    this.mxx = mxx;
    this.mxy = mxy;
    this.mxz = mxz;
    this.myx = myx;
    this.myy = myy;
    this.myz = myz;
  }
  
  public double getMxx() {
    return mxx;
  }
  
  public double getMxy() {
    return mxy;
  }
  
  public double getMxz() {
    return mxz;
  }
  
  public double getMyx() {
    return myx;
  }
  
  public double getMyy() {
    return myy;
  }
  
  public double getMyz() {
    return myz;
  }
  
  public void map(double[] in, double[] out) {
    double x = in[0];
    double y = in[1];
    
    out[0] = mxx * x + mxy * y + mxz;
    out[1] = myx * x + myy * y + myz;
  }
  
  public AffineTransform invert() {
    double invDet = 1 / (mxx * myy - mxy * myx);
    
    double nxx = invDet * myy;
    double nxy = -invDet * mxy;
    double nyx = -invDet * myx;
    double nyy = invDet * mxx;
    
    double nxz = -(nxx * mxz + nxy * myz);
    double nyz = -(nyx * mxz + nyy * myz);
    
    return new AffineTransform(nxx, nxy, nxz, nyx, nyy, nyz);
  }
  
  public AffineTransform after(AffineTransform t) {
    double nxx = mxx * t.mxx + mxy * t.myx;
    double nxy = mxx * t.mxy + mxy * t.myy;
    double nxz = mxx * t.mxz + mxy * t.myz + mxz;
    
    double nyx = myx * t.mxx + myy * t.myx;
    double nyy = myx * t.mxy + myy * t.myy;
    double nyz = myx * t.mxz + myy * t.myz + myz;
    
    return new AffineTransform(nxx, nxy, nxz, nyx, nyy, nyz);
  }
  
  public String toString() {
    return "[" + mxx + ", " + mxy + ", " + mxz + "; " + myx + ", " + myy + ", " + myz + "]";
  }
  
  public static AffineTransform random() {
    return new AffineTransform(Math.random(), Math.random(), Math.random(), Math.random(),
        Math.random(), Math.random());
  }
  
  public static double error(double[] a, double[] b) {
    return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));
  }
  
  public static void main(String[] args) {
    AffineTransform s = random();
    AffineTransform t = random();
    
    double[] in = new double[] {Math.random(), Math.random()};
    double[] out = new double[2];
    double[] out2 = new double[2];
    double[] out3 = new double[2];
    
    s.map(in, out);
    t.map(out, out2);
    
    t.after(s).map(in, out3);
    
    System.err.println(error(out2, out3));
  }
}
