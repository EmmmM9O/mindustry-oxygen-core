varying vec2 v_texCoords;

uniform sampler2D u_texture0;
uniform float u_threshold;

#include "/utils/luminance.glsl"

void main() {
    vec4 color = texture2D(u_texture0, v_texCoords);
    float luminance = luminance(color.xyz);
    luminance = max(luminance - u_threshold, 0.0);

    gl_FragColor = vec4(color.rgb * sign(luminance), 1.0);
}
