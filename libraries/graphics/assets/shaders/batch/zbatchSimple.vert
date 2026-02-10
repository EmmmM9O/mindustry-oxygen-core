attribute vec3 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec4 a_mix_color;
attribute vec4 a_scl_color;

uniform mat4 u_trans;
uniform mat4 u_proj;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_proj * u_trans * vec4(a_position, 1.0);
}
