const float PI = 3.14159265359;
const float EPSILON = 0.0001;
const float INFINITY = 1000000.0;
const vec3 black_hole_pos = vec3(0.0);
const float horizon_radius = 1.0f;
const float adisk_inner_radius = 2.6;
const float adisk_outer_radius = 12.0;

vec3 accel(float h2, vec3 pos) { //引力透镜
    vec3 tmp = pos - black_hole_pos;
    float r2 = dot(tmp, tmp);
    float r5 = pow(r2, 2.5);
    vec3 acc = -1.5 * h2 * pos / r5 * 1.0;
    return acc;
}
void adisk_color(vec3 pos, inout vec3 color, inout float alpha) { //吸积盘
}
vec3 ray_marching(vec3 pos, vec3 dir) {
    vec3 color = vec3(0.0);
    float alpha = 1.0;

    float STEP_SIZE = 0.1;
    dir *= STEP_SIZE;
    vec3 h = cross(pos, dir);
    float h2 = dot(h, h);
    const int max_steps = 200;
    for (int i = 0; i < max_steps; i++) {
        dir += accel(h2, pos); //引力透镜
        if (length(pos - black_hole_pos) < horizon_radius) {
            //到达事件视界
            return color;
        }
        adisk_color(pos, color, alpha); //吸积盘
        pos += dir;
    }
    //宇宙背景
    dir = rotate_vector(dir, vec3(0.0, 1.0, 0.0), time);
    color += texture(galaxy, dir).rgb * alpha;
    return color;
}
