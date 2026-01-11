varying vec4 v_color;
varying vec2 v_texCoords;
varying vec3 v_worldPos;
varying vec4 v_lightSpacePos;

uniform sampler2D u_texture;
uniform sampler2D u_shadowMap;
uniform vec3 u_lightDir;

#include "/utils/zbatch.glsl"

void main() {
    float shadow = pcfShadow(u_shadowMap, v_lightSpacePos.xyz);
    vec4 color = v_color * texture2D(u_texture, v_texCoords);
    vec3 normal = vec3(0.0, 0.0, 1.0);
    BATCHRENDERER
}
