varying vec2 v_texCoords;

uniform float u_exposure;
uniform float u_scale;
uniform float u_intensity;
uniform float u_gamma;
uniform int u_mode;

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;

#include "/utils/tonemap.glsl"

void main() {
    vec4 ori = texture2D(u_texture0, v_texCoords);
    vec3 color = ori.rgb + texture2D(u_texture1, v_texCoords).rgb * u_intensity;
    color *= u_exposure;
    gl_FragColor = vec4(pow(tonemapMode(color.rgb, u_mode), vec3(1.0 / u_gamma)) * u_scale, ori.a);
}
