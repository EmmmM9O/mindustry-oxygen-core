@import(universe/blackhole_base.frag);
uniform sampler2D origin;
vec3 add_color(vec3 pos, vec3 dir, vec3 color, float alpha) {
    return texture(origin, uv).rgb * alpha;
}
