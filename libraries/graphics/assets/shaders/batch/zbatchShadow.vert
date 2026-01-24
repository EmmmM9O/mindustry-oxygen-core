attribute vec3 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec4 a_mix_color;
attribute vec4 a_scl_color;

uniform mat4 u_trans;
uniform mat4 u_proj;
uniform mat4 u_lightProj;

varying vec4 v_color;
varying vec4 v_mix_color;
varying vec4 v_scl_color;
varying vec2 v_texCoords;
varying vec3 v_worldPos;
varying vec4 v_lightSpacePos;

#include "/utils/sclColor.glsl"

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0 / 254.0);
    v_mix_color = a_mix_color;
    v_mix_color.a *= (255.0 / 254.0);
    v_texCoords = a_texCoord0;
    v_scl_color = a_scl_color * color_scl;

    vec4 worldPos = u_trans * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;

    gl_Position = u_proj * worldPos;
    v_lightSpacePos = u_lightProj * worldPos;

    v_lightSpacePos.xy /= v_lightSpacePos.w;
    v_lightSpacePos.xyz = v_lightSpacePos.xyz * 0.5 + 0.5;
}
