attribute vec3 a_position;
attribute vec3 a_normal;

uniform mat4 u_normalMat;
uniform mat4 u_trans;
uniform mat4 u_proj;
uniform mat4 u_lightProj;

varying vec3 v_normal;
varying vec3 v_worldPos;
varying vec4 v_lightSpacePos;

void main() {
    vec4 worldPos = u_trans * vec4(a_position, 1.0);
    v_worldPos = worldPos.xyz;
    v_normal = normalize(mat3(u_normalMat) * a_normal);

    gl_Position = u_proj * worldPos;
    v_lightSpacePos = u_lightProj * worldPos;

    v_lightSpacePos.xy /= v_lightSpacePos.w;
    v_lightSpacePos.xyz = v_lightSpacePos.xyz * 0.5 + 0.5;
}
