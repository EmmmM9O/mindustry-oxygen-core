varying lowp vec4 v_color;
varying lowp vec4 v_mix_color;
varying highp vec2 v_texCoords;
varying vec3 v_worldPos;
varying vec4 v_lightSpacePos;

uniform highp sampler2D u_texture;
uniform sampler2D u_shadowMap;
uniform float u_alphaTest;
uniform vec3 u_lightDir;

#include "/utils/zbatch.glsl"

void main() {
    vec4 c = texture2D(u_texture, v_texCoords);
    vec4 color = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);
    if (color.a < u_alphaTest)
        discard;
    float shadow = pcfShadow(u_shadowMap, v_lightSpacePos.xyz);
    vec3 normal = vec3(0.0, 0.0, 1.0);
    BATCHRENDERER
}
