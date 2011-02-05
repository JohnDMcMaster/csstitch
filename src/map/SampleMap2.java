package map;

import java.io.Serializable;

import map.properties.ImagePosition;
import map.properties.ImageSize;
import map.properties.OpticalProperties;
import map.properties.PerspectiveProperties;
import map.properties.StitchStackProperties;

public class SampleMap2 implements Map, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private double icx, icy;
  private double radialA, radialB;
  private double perspectiveX, perspectiveY;
  
  private double mxx, mxy, mxz, myx, myy, myz;
  private double nxx, nxy, nxz, nyx, nyy, nyz;
  
  public SampleMap2(ImageSize size, OpticalProperties opticalProperties,
      PerspectiveProperties perspectiveProperties, AffineTransform transform) {
    if (!(opticalProperties.getNumCoefs() == 2))
      throw new IllegalArgumentException("optical correction model not supported");
    
    icx = 0.5 * size.getSx();
    icy = 0.5 * size.getSy();
    
    radialA = opticalProperties.getCoef(0);
    radialB = opticalProperties.getCoef(1);
    
    perspectiveX = perspectiveProperties.getCoef(0);
    perspectiveY = perspectiveProperties.getCoef(1);
    
    mxx = transform.getMxx();
    mxy = transform.getMxy();
    mxz = transform.getMxz();
    myx = transform.getMyx();
    myy = transform.getMyy();
    myz = transform.getMyz();
    
    transform = transform.invert();
    nxx = transform.getMxx();
    nxy = transform.getMxy();
    nxz = transform.getMxz();
    nyx = transform.getMyx();
    nyy = transform.getMyy();
    nyz = transform.getMyz();
  }
  
  public void map(double[] in, double[] out) {
    double x = in[0] - icx;
    double y = in[1] - icy;
    
    double rr = x * x + y * y;
    double radial = radialA + rr * radialB;
    double perspective = x * perspectiveX + y * perspectiveY;
    double factor = radial * (1 + radial * perspective);
    
    x *= factor;
    y *= factor;
    
    out[0] = mxx * x + mxy * y + mxz;
    out[1] = myx * x + myy * y + myz;
  }
  
  public void unmap(double[] in, double[] out) {
    double x = nxx * in[0] + nxy * in[1] + nxz;
    double y = nyx * in[0] + nyy * in[1] + nyz;
    
    double perspectiveInvFactor =
        1 / (Math.sqrt((x * perspectiveX + y * perspectiveY) + 0.25) + 0.5);
    
    x *= perspectiveInvFactor;
    y *= perspectiveInvFactor;
    
    if (!(Math.abs(x) < 2 * icx && Math.abs(y) < 2 * icy)) {
      out[0] = 1E9 * Math.signum(x);
      out[1] = 1E9 * Math.signum(y);
      return;
    }
    
    double rrr = x * x + y * y;
    double a0 = radialA;
    double b0 = rrr * radialB;
    double a1 = 1 * radialA;
    double b1 = 3 * rrr * radialB;
    
    // Newton
    double radialInvFactor = 1;
    for (;;) {
      double sq = radialInvFactor * radialInvFactor;
      double next = radialInvFactor - (-1 + radialInvFactor * (a0 + sq * b0)) / (a1 + sq * b1);
      if (Math.abs(1 - radialInvFactor / next) < 1E-15)
        break;
      
      radialInvFactor = next;
    }
    
    x *= radialInvFactor;
    y *= radialInvFactor;
    
    out[0] = x + icx;
    out[1] = y + icy;
  }
  
}
