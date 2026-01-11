attribute vec3 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec4 a_mix_color;

uniform mat4 u_trans;
uniform mat4 u_proj;

varying float v_depth;
varying vec2 v_texCoords;

void main() {
    v_texCoords = a_texCoord0;
    gl_Position = u_proj * u_trans * vec4(a_position, 1.0);
    v_depth = gl_Position.z;
}
