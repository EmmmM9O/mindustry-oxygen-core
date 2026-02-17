varying vec3 v_normal;
varying vec3 v_worldPos;
varying vec4 v_lightSpacePos;

uniform sampler2D u_shadowMap;
uniform vec3 u_lightDir;
uniform vec3 u_camPos;

#include "/utils/diffuse.glsl"
#include "/utils/shadow.glsl"
#include "/utils/zbatch.glsl"

void main() {
    float shadow = pcfShadow(u_shadowMap, v_lightSpacePos.xyz);
    float diff = halfLambert(v_normal, -u_lightDir);
    vec3 color = vec3(0.8) * (ambientIntensity + diff * (1.0 - ambientIntensity) * (1.0 - shadow * shadowIntensity));
    gl_FragColor = vec4(color, 1.0);
}
