varying vec2 v_texCoords;

uniform sampler2D u_texture0;
uniform vec2 u_resolution;
uniform float u_iteration;

// Karis平均
vec3 karisAverage(vec3 color) {
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    float weight = 1.0 / (1.0 + luma);
    return color * weight;
}

vec3 fetch(vec2 texCoords, vec2 texel) {
    vec4 color = vec4(0.0);
    color += texture2D(u_texture0, texCoords + vec2(-1.0, -1.0) * texel);
    color += texture2D(u_texture0, texCoords + vec2(1.0, -1.0) * texel);
    color += texture2D(u_texture0, texCoords + vec2(-1.0, 1.0) * texel);
    color += texture2D(u_texture0, texCoords + vec2(1.0, 1.0) * texel);

    color *= 0.25;
#ifdef KARIS_AVERAGE
    return karisAverage(color.rgb);
#else
    return color.rgb;
#endif
}

void main() {
    vec2 texel = 1.0 / u_resolution * 0.5;
    float scl = (u_iteration + 0.5) * 2.0;
    vec2 offs = texel * scl;
    vec3 color = vec3(0.0);
    color += fetch(v_texCoords, texel) * 0.5;
    color += fetch(v_texCoords + vec2(1.0, 1.0) * offs, texel) * 0.125;
    color += fetch(v_texCoords + vec2(-1.0, 1.0) * offs, texel) * 0.125;
    color += fetch(v_texCoords + vec2(1.0, -1.0) * offs, texel) * 0.125;
    color += fetch(v_texCoords + vec2(-1.0, -1.0) * offs, texel) * 0.125;
    gl_FragColor = vec4(color, 1.0);
}
