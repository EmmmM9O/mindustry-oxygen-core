varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2; //Ignore
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;

void main() {
    gl_FragColor = 0.25 * (texture2D(u_texture0, v_texCoords0) + texture2D(u_texture0, v_texCoords1) +
                           texture2D(u_texture0, v_texCoords3) + texture2D(u_texture0, v_texCoords4));
    gl_FragColor += texture2D(u_texture1, v_texCoords2);
    gl_FragColor.a = 1.0;
}
