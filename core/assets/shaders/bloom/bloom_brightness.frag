#define HIGHP
varying vec2 uv;

uniform sampler2D texture0;
uniform vec2 resolution;
uniform float threshold;
const vec3 luminanceVector = vec3(0.2126, 0.7152, 0.0722);
void main() {
    vec2 texCoord = gl_FragCoord.xy / resolution.xy;

    vec4 c = texture2D(texture0, texCoord);
    float luminance = dot(luminanceVector, c.xyz);
    luminance = max(0.0, luminance - threshold);
    c.xyz *= sign(luminance);
    c.a = 1.0;

    gl_FragColor = c;
}
