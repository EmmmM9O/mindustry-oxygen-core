package com.github.emmmm9o.oxygencore.universe;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.struct.Seq;

/**
 * Orbit
 */
public class Orbit {
  /* GNSS */
  // orbital elements
  public float semimajor_axis, eccentricity, inclination,
      periapsis /* argument of periapsis 近日点俯角 */, ascending /* longitude of ascending node 升交点 */
  /* starts */;
  public OPlanet parent;
  public Color color = Color.white;
  public float orginMean;

  /*
   * 半长轴 a：椭圆轨道长轴的一半，有时可视作平均轨道半径。
   * 离心率 e ：为椭圆扁平程度的一种量度，定义是椭圆两焦点间的距离与长轴长度的比值。 就是 [公式] 。
   * 轨道倾角 i ：行星轨道面对赤道面的倾角；在升交点处从赤道面逆时针方向量到行星轨道面的角度。
   * 近日点辐角 ω：从升交点沿行星运动轨道逆时针量到近地点的角度。
   * 升交点黄经 Ω：行星轨道升交点的黄道经度。
   * 在指定历元的平近点角 M0 ：行星对应于 初始时该的平近点角。
   */
  public Orbit(
      float semimajor_axis, float eccentricity, float inclination,
      float periapsis, float ascending,
      float mean_anomaly, OPlanet planet) {
    this.semimajor_axis = semimajor_axis;
    this.eccentricity = eccentricity;
    this.inclination = inclination;
    this.periapsis = periapsis;
    this.ascending = ascending;
    this.mean_anomaly = this.orginMean = mean_anomaly;
    this.parent = planet;
    calculation();
    get_points();
  }

  /* calculation elements */
  public float period;
  public float mean_anomaly, eccentric_anomaly;/* start time */
  /*
   * public float cal_eccentric_anomaly() {
   * eccentric_anomaly = 2 * (float) Math
   * .atan(Mathf.sqrt((1 + eccentricity) / (1 - eccentricity)) *
   * Math.tan(true_anomaly / 2));
   * return eccentric_anomaly;
   * }
   * 
   * public float cal_mean_anomaly() {
   * mean_anomaly = eccentric_anomaly - eccentricity *
   * Mathf.sin(eccentric_anomaly);
   * return mean_anomaly;
   * }
   */

  public float cal_period() {
    period = 2 * Mathf.pi * Mathf.sqrt(semimajor_axis * semimajor_axis * semimajor_axis /
        parent.gravitational_parameter());
    return period;
    // 开普勒第三定律
  }

  public void calculation() {
    cal_period();

  }

  public float l_mean_anomaly, l_eccentric_anomaly, l_true_anonaly, l_time;

  public float cal_l_mean_anomaly(float time) {
    l_mean_anomaly = mean_anomaly + 2 * Mathf.pi * time / period;
    return l_mean_anomaly;
  }

  public float cal_l_eccentric_anomaly() {
    float E = l_mean_anomaly;
    float deltaE;
    // M=E- e*sin E
    for (int i = 0; i <= 200/* 最大迭代次数 */; i++) {
      float f = E - eccentricity * (float) Math.sin(E) - l_mean_anomaly;
      float fPrime = 1f - eccentricity * (float) Math.cos(E);
      deltaE = f / fPrime;
      E -= deltaE;
      if (Math.abs(deltaE) < 1e-8/* 精度 */) {
        break;
      }
    }
    l_eccentric_anomaly = E;
    return l_eccentric_anomaly;
  }

  public float cal_l_true_anomaly() {
    l_true_anonaly = 2
        * (float) Math.atan(Mathf.sqrt((1 + eccentricity) / (1 - eccentricity)) * Math.tan(l_eccentric_anomaly / 2));
    return l_true_anonaly;
  }

  public void calculation_t(float time/* second */) {
    l_time = time;
    cal_l_mean_anomaly(time);
    cal_l_eccentric_anomaly();
    cal_l_true_anomaly();
  }

  public Vec3 calculatePosition(float v) {
    float r = (semimajor_axis * (1 - eccentricity * eccentricity))
        / (1 + eccentricity * Mathf.cos(v));
    float xPrime = r * Mathf.cos(v);
    float yPrime = r * Mathf.sin(v);
    float Omega = ascending;
    float i = inclination;
    float omega = periapsis;
    float cosOmega = Mathf.cos(Omega);
    float sinOmega = Mathf.sin(Omega);
    float cosI = Mathf.cos(i);
    float sinI = Mathf.sin(i);
    float cosOmegaW = Mathf.cos(omega);
    float sinOmegaW = Mathf.sin(omega);
    float x = cosOmega * (xPrime * cosOmegaW - yPrime * sinOmegaW)
        - (xPrime * sinOmegaW + yPrime * cosOmegaW) * cosI * sinOmega;
    float y = sinOmega * (xPrime * cosOmegaW - yPrime * sinOmegaW)
        + (xPrime * sinOmegaW + yPrime * cosOmegaW) * cosI * cosOmega;
    float z = sinI * (xPrime * sinOmegaW + cosOmegaW * yPrime);
    /*
     *
     * */
    return new Vec3(x, z, y);
  }

  public Vec3 calculatePositionT(float time) {
    calculation_t(time);
    return calculatePosition(l_true_anonaly);
  }

  public Seq<Vec3> points;
  public int points_num = 100;

  public void get_points() {
    points = new Seq<Vec3>();
    for (int j = 0; j < points_num; j++) {
      points.add(calculatePosition(2 * Mathf.pi * j / points_num));
    }
  }
}
