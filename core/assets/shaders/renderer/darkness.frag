varying lowp vec4 v_color;
varying highp vec2 v_texCoords;

uniform highp sampler2D u_texture0;

void main() {
    vec4 c = texture2D(u_texture0, v_texCoords);
    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0 - c.r);
}
