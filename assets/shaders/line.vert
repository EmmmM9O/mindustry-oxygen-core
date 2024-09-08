@import("transpose");
@import("inverse");
attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

varying vec3 v_position;
varying vec3 v_normal;
varying float v_radius;
varying float v_zoom;

uniform mat4 u_projection;
uniform mat4 u_model;
uniform mat4 u_view;
uniform int u_no_cloud;
uniform float u_zoom;
uniform float u_radius;

void main() {
    v_radius = u_radius;
    v_zoom = u_zoom;
    vec4 pos = a_position;
    pos.x *= u_radius;
    pos.y *= u_radius;
    pos.z *= u_radius;
    v_position = vec3(u_model * pos);

    v_normal = normalize(mat3(transpose(inverse(u_model))) * a_normal);
    gl_Position = u_projection * u_view * vec4(v_position, 1.0f);
}
