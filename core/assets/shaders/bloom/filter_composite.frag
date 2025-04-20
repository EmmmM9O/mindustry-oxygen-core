
//reference:https://github.com/ebruneton/black_hole_shader
//RENDER_SHADER
varying vec2 uv;
const int SIZE = 25;
uniform float exposure;
uniform float intensity;
uniform vec3 source_samples_uvw[SIZE];

uniform sampler2D texture0;
uniform sampler2D texture1;

void main() {
    vec3 color = texture(texture1, uv).rbg;
    for (int i = 0; i < SIZE; ++i) {
        vec3 uvw = source_samples_uvw[i];
        color += uvw.z * texture(texture0, uv + uvw.xy).rgb;
    }
    color = mix(texture(texture0, uv).rbg, color, intensity) * exposure;
    color = min(color, 10.0);
    gl_FragColor = vec4(color, 1.0);
}
