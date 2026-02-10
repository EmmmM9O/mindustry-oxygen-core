varying vec2 v_texCoords;

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform vec2 u_resolution;
uniform float u_iteration;

void main() {
    vec2 texel = 1.0 / u_resolution * (u_iteration + 0.5) * 2.0;
    vec4 color = vec4(0.0);
    color += texture2D(u_texture0, v_texCoords) * 4.0;

    color += texture2D(u_texture0, v_texCoords + vec2(1.0, 1.0) * texel);
    color += texture2D(u_texture0, v_texCoords + vec2(-1.0, -1.0) * texel);
    color += texture2D(u_texture0, v_texCoords + vec2(1.0, -1.0) * texel);
    color += texture2D(u_texture0, v_texCoords + vec2(-1.0, 1.0) * texel);

    color += texture2D(u_texture0, v_texCoords + vec2(0.0, 1.0) * texel) * 2.0;
    color += texture2D(u_texture0, v_texCoords + vec2(1.0, 0.0) * texel) * 2.0;
    color += texture2D(u_texture0, v_texCoords + vec2(0.0, -1.0) * texel) * 2.0;
    color += texture2D(u_texture0, v_texCoords + vec2(-1.0, 0.0) * texel) * 2.0;

    color /= 16.0;
    color += texture2D(u_texture1, v_texCoords);

    gl_FragColor = vec4(color.rgb, 1.0);
}
