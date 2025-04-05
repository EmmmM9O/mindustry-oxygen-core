package oxygen.graphics.postprocessing;


public interface BufferCapturable {
  public abstract void capture();

  public abstract void capturePause();

  public abstract void captureContinue();
}
