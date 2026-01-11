varying float v_depth;

void main() {
    float depth = v_depth * 0.5 + 0.5;
    gl_FragColor = vec4(vec3(depth), 1.0);
}
