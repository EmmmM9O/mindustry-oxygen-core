attribute vec3 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec4 a_mix_color;
attribute vec4 a_scl_color;

uniform mat4 u_trans;
uniform mat4 u_proj;

varying vec4 v_color;
varying vec4 v_mix_color;
varying vec4 v_scl_color;
varying vec2 v_texCoords;

#include "/utils/sclColor.glsl"

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0 / 254.0);
    v_scl_color = a_scl_color * color_scl;
    v_mix_color = a_mix_color;
    v_mix_color.a *= (255.0 / 254.0);
    v_texCoords = a_texCoord0;
    gl_Position = u_proj * u_trans * vec4(a_position, 1.0);
}
