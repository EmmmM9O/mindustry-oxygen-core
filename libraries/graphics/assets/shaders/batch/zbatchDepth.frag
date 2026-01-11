varying vec2 v_texCoords;
varying float v_depth;

uniform sampler2D u_texture;
uniform float u_alphaTest;

void main() {
    vec4 c = texture2D(u_texture, v_texCoords);
    if (c.a < u_alphaTest)
        discard;
    float depth = v_depth * 0.5 + 0.5;
    gl_FragColor = vec4(vec3(depth), 1.0);
}
