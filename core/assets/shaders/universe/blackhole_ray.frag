#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform mat3 camera_mat;
uniform float horizon_radius;
uniform float horizon_radius_2;
uniform float max_scl;
uniform float min_scl;
uniform float scl_r;
uniform float scl_t;
uniform float fov_scale;
uniform float max_steps;
uniform float step_size;
uniform float a_distance;

uniform float adisk_inner_radius;
uniform float adisk_outer_radius;
uniform float adisk_height;
uniform float adisk_density_v;
uniform float adisk_density_h;
vec3 accel(float h2, vec3 pos) { //引力透镜
    float r2 = dot(pos, pos);
    float r5 = pow(r2, 2.5);
    vec3 acc = -1.5 * h2 * pos / r5 * 1.0;
    return acc;
}

vec4 ray_marching(vec3 pos, vec3 dir) {
    dir *= step_size;
    vec3 h = cross(pos, dir);
    float p = length(h) / length(dir);
    if (p >= a_distance) {
        return vec4((normalize(dir) + 1.0) / 2.0, 0.0);
    }
    if (p <= horizon_radius) {
        return vec4(0.0);
    }
    float h2 = dot(h, h);
    float to = (length(pos) - aDistance);
    float den = 0.0;
    if (to >= 0.0)
        pos += dir * to;
    for (int i = 0; i < int(max_steps); i++) {
        float len = dot(pos, pos);
        float tlen = inversesqrt(len);
        float scl = mix(min_scl, max_scl, 1.0 - smoothstep(0.0f, scl_t, tlen / scl_r));

        dir += accel(h2, pos) * scl;
        float density = max(
                0.0, 1.0 - length(pos.xyz / vec3(adisk_outer_radius, adisk_height, adisk_outer_radius))) * scl;

        density *= pow(1.0 - abs(pos.y) / adisk_height, adisk_density_v);
        density *= smoothstep(adisk_inner_radius, adisk_inner_radius * 1.1, length(pos));
        density *= 1.0 / pow(tlen, adisk_density_h);
        den += density;
        if (len <= horizon_radius_2) {
            return vec4(0.0);
        }
        pos += dir * scl;
    }
    return vec4((normalize(dir) + 1.0) / 2.0, abs(den / 100));
}

void main() {
    vec2 uv = (gl_FragCoord.xy + jitter) / resolution - vec2(0.5);
    uv.x *= resolution.x / resolution.y;
    vec3 ray_dir = normalize(vec3(-uv.x * fov_scale, uv.y * fov_scale, 1.0));
    vec3 dir = camera_mat * ray_dir;
    return ray_marching(camera_pos, dir);
}
