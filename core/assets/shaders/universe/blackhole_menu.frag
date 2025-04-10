@import(universe/blackhole_base.frag);
vec3 add_color(vec3 pos, vec3 dir, vec3 color, float alpha){
  return texture(galaxy, dir).rgb * alpha;
}
