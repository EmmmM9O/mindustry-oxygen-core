#define HIGHP
varying vec2 uv;
uniform float border;
uniform float size;
uniform sampler2D texture0;
float DOWNSAMPLE[4][4] = float[4][4](
        float[4](
            1.0 / 81.0,
            3.0 / 81.0,
            3.0 / 81.0,
            1.0 / 81.0
        ),
        float[4](
            3.0 / 81.0,
            9.0 / 81.0,
            9.0 / 81.0,
            3.0 / 81.0
        ),
        float[4](
            3.0 / 81.0,
            9.0 / 81.0,
            9.0 / 81.0,
            3.0 / 81.0
        ),
        float[4](
            1.0 / 81.0,
            3.0 / 81.0,
            3.0 / 81.0,
            1.0 / 81.0
        )
    );

void main() {
    vec2 xy = gl_FragCoord.xy - border;
    float scl = size * 2.0 + (border + 1) * 2.0 * 2.0;
    float value = 0.0;
    for (int dy = 0; dy < 4; ++dy) {
        for (int dx = 0; dx < 4; ++dx) {
            value += texture(texture0, (2.0 * xy + vec2(dx, dy) - 1.0) / scl).r * DOWNSAMPLE[dx][dy];
        }
    }
    gl_FragColor = vec4(value, 0.0, 0.0, 1.0);
}
