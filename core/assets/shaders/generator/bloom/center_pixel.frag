#define HIGHP
varying vec2 uv;
uniform float border;
uniform float size;

void main() {
    vec2 dxy = gl_FragCoord.xy - border - size / 2.0;
    gl_FragColor = vec4(int(dxy.x) == 0 && int(dxy.y) == 0 ? 1.0 : 0.0, 0.0, 0.0, 1.0);
}
