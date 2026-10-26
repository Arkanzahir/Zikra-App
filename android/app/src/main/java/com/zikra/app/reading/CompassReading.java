package com.zikra.app.reading;

/** Confidence state uses the sensor's monotonic timestamp, never wall-clock time. */
public final class CompassReading {
  public static final long MAX_AGE_MS = 3000;
  public enum Status { WAITING, INVALID, STALE, TILTED, LOW_ACCURACY, LIVE }
  private long timestamp;
  private float heading;
  private boolean measured, reliable, flat, aligned, invalid;

  public void reset() {
    timestamp=0; heading=0; measured=false; reliable=false; flat=false; aligned=false; invalid=false;
  }
  public void invalidate() { invalid=true; aligned=false; }
  public void downgradeAccuracy() { reliable=false; aligned=false; }
  public boolean accept(float trueHeading, float target, boolean reliable, boolean flat,
      long timestamp, long now) {
    if(!Float.isFinite(trueHeading)||!Float.isFinite(target)||timestamp<=0
        ||timestamp>now||now-timestamp>MAX_AGE_MS){invalidate();return false;}
    if(timestamp<=this.timestamp)return false;
    boolean continuing=status(now)==Status.LIVE&&reliable&&flat;
    heading=continuing?CompassMath.smooth(heading,trueHeading,0.2f):CompassMath.normalize(trueHeading);
    this.timestamp=timestamp;this.measured=true;this.invalid=false;
    this.reliable=reliable;this.flat=flat;
    float distance=Math.abs(CompassMath.delta(target,heading));
    aligned=reliable&&flat&&distance<=(continuing&&aligned?8f:5f);
    return true;
  }
  public Status status(long now) {
    if(invalid)return Status.INVALID;
    if(!measured)return Status.WAITING;
    if(now<timestamp||now-timestamp>MAX_AGE_MS)return Status.STALE;
    if(!flat)return Status.TILTED;
    if(!reliable)return Status.LOW_ACCURACY;
    return Status.LIVE;
  }
  public float heading() { return heading; }
  public boolean aligned(long now) { return status(now)==Status.LIVE&&aligned; }
  public static boolean validVector(float[] vector) {
    if(vector==null||vector.length<3)return false;
    double norm=0;
    for(int i=0;i<Math.min(vector.length,4);i++){
      if(!Float.isFinite(vector[i]))return false;
      norm+=(double)vector[i]*vector[i];
    }
    return vector.length==3?norm<=1.01:norm>=0.8&&norm<=1.2;
  }
}
