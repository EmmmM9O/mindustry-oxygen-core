attribute vec3 a_position;
attribute vec3 a_normal;

uniform mat4 u_trans;
uniform mat4 u_proj;

varying float v_depth;

void main() {
    gl_Position = u_proj * u_trans * vec4(a_position, 1.0);
    v_depth = gl_Position.z;
}
