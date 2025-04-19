@import(util/noise.glsl);
#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform mat3 camera_mat;
uniform samplerCube galaxy;
uniform sampler2D color_map;
uniform float time;

uniform float max_length_2;
uniform float horizon_radius_2;
uniform float adisk_inner_radius;
uniform float adisk_outer_radius;
uniform float adisk_height;
uniform float adisk_density_v;
uniform float adisk_density_h;
uniform float adisk_noise_scale;
uniform float adisk_noise_LOD;
uniform float adisk_speed;
uniform float adisk_lit;
uniform float adisk_particle;
uniform float max_scl;
uniform float min_scl;
uniform float scl_r;
uniform float scl_t;
uniform float max_steps;
uniform float step_size;
uniform float fov_scale;
uniform float a_distance;

float length_black_hole(vec3 pos) {
    return dot(pos, pos);
}
vec3 accel(float h2, vec3 pos) { //引力透镜
    float r2 = length_black_hole(pos);
    float r5 = pow(r2, 2.5);
    vec3 acc = -1.5 * h2 * pos / r5 * 1.0;
    return acc;
}
// Convert from Cartesian to spherical coord (rho, phi, theta)
// https://en.wikipedia.org/wiki/Spherical_coordinate_system
vec3 toSpherical(vec3 p) {
    float rho = sqrt((p.x * p.x) + (p.y * p.y) + (p.z * p.z));
    float theta = atan(p.z, p.x);
    float phi = asin(p.y / rho);
    return vec3(rho, theta, phi);
}

///---- from https://github.com/rossning92/Blackhole
void adisk_color(vec3 pos, inout vec3 color, inout float alpha, float scl) { //吸积盘
    //密度

    float density = max(
            0.0, 1.0 - length(pos.xyz / vec3(adisk_outer_radius, adisk_height, adisk_outer_radius))) * scl;
    if (density < 0.001) {
        return;
    }
    density *= pow(1.0 - abs(pos.y) / adisk_height, adisk_density_v);
    density *= smoothstep(adisk_inner_radius, adisk_inner_radius * 1.1, length(pos));
    if (density < 0.001) {
        return;
    }
    vec3 sphericalCoord = toSpherical(pos);
    sphericalCoord.y *= 2.0;
    sphericalCoord.z *= 4.0;

    density *= 1.0 / pow(sphericalCoord.x, adisk_density_h);
    density *= 16000.0;

    float noise = 1.0;
    for (int i = 0; i < int(adisk_noise_LOD); i++) {
        noise *= 0.5 * snoise((sphericalCoord * adisk_noise_scale) * float(i * i)) + 0.5;
        if (i % 2 == 0) {
            sphericalCoord.y += time * adisk_speed;
        } else {
            sphericalCoord.y -= time * adisk_speed;
        }
    }

    vec3 dustColor =
        texture(color_map, vec2(sphericalCoord.x / adisk_outer_radius, 0.5)).rgb;

    color += density * adisk_lit * dustColor * alpha * abs(noise);
}
vec3 add_color(vec3 pos, vec3 dir, vec3 color, float alpha);
vec3 ray_marching(vec3 pos, vec3 dir) {
    vec3 color = vec3(0.0);
    float alpha = 1.0;

    dir *= step_size;
    vec3 h = cross(pos, dir);
    float p = length(h) / length(dir);
    if (p >= a_distance) {
        color += add_color(pos, dir, color, alpha);
        return color;
    }
    float h2 = dot(h, h);
    float to = (length(pos) - a_distance);
    if (to >= 0.0)
        pos += dir * to;
    for (int i = 0; i < int(max_steps); i++) {
        float len = length_black_hole(pos);
        float scl = mix(min_scl, max_scl, 1.0 - smoothstep(0.0f, scl_t, inversesqrt(len) / scl_r));
        if (len >= max_length_2)
            break;
        dir += accel(h2, pos) * scl; //引力透镜
        if (len <= horizon_radius_2) {
            //到达事件视界
            return color;
        }
        adisk_color(pos, color, alpha, scl); //吸积盘
        pos += dir * scl;
    }
    color += add_color(pos, dir, color, alpha);
    return color;
}

void main() {
    vec3 pos = camera_pos;
    vec2 jitter = vec2(
            snoise(vec3(gl_FragCoord.xy, time)),
            snoise(vec3(gl_FragCoord.yx, time + 100.0))
        ) * 0.005 / resolution;
    vec2 uv = (gl_FragCoord.xy + jitter) / resolution - vec2(0.5);
    uv.x *= resolution.x / resolution.y;
    vec3 rayDir = normalize(vec3(-uv.x * fov_scale, uv.y * fov_scale, 1.0));
    vec3 dir = rayDir * camera_mat;
    gl_FragColor = vec4(ray_marching(pos, normalize(dir)), 1.0f);
}
