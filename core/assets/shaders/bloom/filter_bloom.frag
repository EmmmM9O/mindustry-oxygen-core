#import(util/float16.glsl);
#define HIGHP
varying vec2 uv;
//reference:https://github.com/ebruneton/black_hole_shader
//BLOOM_SHADER
uniform sampler2D texture0;
uniform vec2 resolution;
const int SIZE = 25;
uniform vec3 source_samples_uvw[SIZE];
void main() {
    vec2 source_delta_uv = 1.0 / resolution * 0.5;
    vec2 source_uv = (gl_FragCoord.xy + vec2(1.0)) * source_delta_uv;
    vec3 color = vec3(0.0);
    for (int i = 0; i < SIZE; ++i) {
        vec3 uvw = source_samples_uvw[i];
        color += uvw.z * texture2D(texture0, source_uv + uvw.xy).rgb;
    }
    gl_FragColor = vec4(min(color, MAX_FLOAT16), 1.0);
}
