varying vec2 uv;

uniform float exposure;
uniform float intensity;

uniform sampler2D texture0;
uniform sampler2D texture1;

void main() {
    gl_FragColor =
        min(mix(texture2D(texture0, uv), texture2D(texture1, uv), intensity) * exposure,10.0);
}
