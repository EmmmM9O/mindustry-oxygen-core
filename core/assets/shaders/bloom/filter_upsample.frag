#import(util/float16.glsl);
#define HIGHP
varying vec2 uv;
//reference:https://github.com/ebruneton/black_hole_shader
//UPSAMPLE_SHADER
const vec4 WEIGHTS[4] = vec4[4](
        vec4(1.0, 3.0, 3.0, 9.0) / 16.0,
        vec4(3.0, 1.0, 9.0, 3.0) / 16.0,
        vec4(3.0, 9.0, 1.0, 3.0) / 16.0,
        vec4(9.0, 3.0, 3.0, 1.0) / 16.0
    );
uniform sampler2D texture0;
uniform vec2 resolution;
void main() {
    vec2 source_delta_uv = 1.0 / resolution * 0.5;
    vec2 ij = floor(gl_FragCoord.xy);
    vec2 source_ij = floor((ij - vec2(1.0)) * 0.5) + vec2(0.5);
    vec2 source_uv = source_ij * source_delta_uv;
    vec3 c0 = texture(texture0, source_uv).rgb;
    vec3 c1 = texture(texture0, source_uv + vec2(source_delta_uv.x, 0.0)).rgb;
    vec3 c2 = texture(texture0, source_uv + vec2(0.0, source_delta_uv.y)).rgb;
    vec3 c3 = texture(texture0, source_uv + source_delta_uv).rgb;
    vec4 weight = WEIGHTS[int(mod(ij.x, 2.0) + 2.0 * mod(ij.y, 2.0))];
    vec3 color = weight.x * c0 + weight.y * c1 + weight.z * c2 + weight.w * c3;
    gl_FragColor = vec4(min(color, MAX_FLOAT16), 1.0 ) ;
}
