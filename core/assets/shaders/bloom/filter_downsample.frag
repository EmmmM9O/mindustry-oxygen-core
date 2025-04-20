#import(util/float16.glsl);
#define HIGHP
varying vec2 uv;
//reference:https://github.com/ebruneton/black_hole_shader
//DOWNSAMPLE_SHADER
const vec4 WEIGHTS = vec4(1.0, 3.0, 3.0, 1.0) / 8.0;
uniform sampler2D texture0;
uniform vec2 resolution;
void main() {
    vec2 source_delta_uv = 1.0 / resolution * 0.5;
    vec2 ij = floor(gl_FragCoord.xy);
    vec2 source_ij = ij * 2.0 - vec2(1.5);
    vec2 source_uv = source_ij * source_delta_uv;
    vec3 color = vec3(0.0);
    for (int i = 0; i < 4; ++i) {
        float wi = WEIGHTS[i];
        for (int j = 0; j < 4; ++j) {
            float wj = WEIGHTS[j];
            vec2 delta_uv = vec2(i, j) * source_delta_uv;
            color += wi * wj * texture2D(texture0, source_uv + delta_uv).rgb;
        }
    }
    gl_FragColor = vec4(min(color, MAX_FLOAT16), 1.0);
}
