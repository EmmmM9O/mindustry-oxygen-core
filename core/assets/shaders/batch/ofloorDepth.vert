attribute vec2 a_position;
attribute float a_z;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_trans;
uniform mat4 u_proj;

varying float v_depth;

void main() {
    gl_Position = u_proj * u_trans * vec4(a_position.x, a_position.y, a_z, 1.0);
    v_depth = gl_Position.z;
}
