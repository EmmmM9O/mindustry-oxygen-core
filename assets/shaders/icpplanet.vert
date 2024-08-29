attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;
uniform mat4 u_projection;
uniform mat4 u_view;
uniform mat4 u_model;
uniform float u_radius;
varying vec2 v_texCoords;

void main() {
    v_texCoords = a_texCoord0;
    vec4 pos = a_position;
    pos.x *= u_radius;
    pos.y *= u_radius;
    pos.z *= u_radius;
    gl_Position = u_projection * u_view * (u_model * pos);
}
