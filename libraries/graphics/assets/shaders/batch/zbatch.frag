varying lowp vec4 v_color;
varying lowp vec4 v_mix_color;
varying lowp vec4 v_scl_color;
varying highp vec2 v_texCoords;

uniform highp sampler2D u_texture;
uniform int u_dis;

void main() {
    vec4 c = texture2D(u_texture, v_texCoords);
    vec4 color = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a) * v_scl_color;
    if (color.a < 0.9 && u_dis == 1)
        discard;
    gl_FragColor = color;
}
