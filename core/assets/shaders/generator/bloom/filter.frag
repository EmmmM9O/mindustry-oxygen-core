#define HIGHP
varying vec2 uv;
uniform float r0;
uniform float border;
uniform float size;
uniform float integral;
/**
   * Physically-based glare effects for digital images.
   * https://www.graphics.cornell.edu/pubs/1995/SSZG95.html
   */
float filterF(vec2 pos) {
    float r = sqrt(pos.x * pos.x + pos.y * pos.y);
    return pow(0.02 / (r / r0 + 0.02), 3.0);
}
void main() {
    float filtered = filterF(gl_FragCoord.xy - border - size / 2.0) / integral;
    gl_FragColor = vec4(filtered, 0.0, 0.0, 1.0);
}
