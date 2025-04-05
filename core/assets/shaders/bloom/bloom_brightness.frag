varying vec2 uv;

uniform sampler2D texture0;
uniform vec2 resolution;

uniform float threshold;
uniform vec3 luminanceVector;
uniform float softEdgeRange;
void main() {
    vec2 texCoord = gl_FragCoord.xy / resolution.xy;

    vec4 c = texture2D(texture0, texCoord);
    vec3 linearColor = pow(c.rgb, vec3(2.2));
    float luminance = dot(luminanceVector, linearColor);
    luminance = clamp(luminance - threshold, 0.0, 100.0);
    float softEdge = smoothstep(0.0, softEdgeRange, luminance);
    c.rgb = linearColor * softEdge;
    c.a = softEdge;

    gl_FragColor = c;
}
