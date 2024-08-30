@import("transpose");
@import("inverse");
attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord;

uniform mat4 u_projection;
uniform mat4 u_model;
uniform mat4 u_view;
uniform float u_zoom;

void main() {
    v_texCoord = a_texCoord0;
    vec4 pos = a_position;
    pos.x /= u_zoom;
    pos.y /= u_zoom;
    pos.z /= u_zoom;
    v_position = vec3(u_model * pos);
    v_normal = normalize(mat3(transpose(inverse(u_model))) * a_normal);
    gl_Position = u_projection * u_view * vec4(v_position,1.0f);
}
