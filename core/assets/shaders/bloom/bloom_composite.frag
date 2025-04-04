varying vec2 uv;

uniform float tone;
uniform float bloom_strength;

uniform sampler2D texture0;
uniform sampler2D texture1;

void main() {
    gl_FragColor =
        texture2D(texture0, uv) * tone + texture2D(texture1, uv) * bloom_strength;
}
