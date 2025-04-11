#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform vec3 camera_dir;
uniform vec3 camera_up;
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
uniform float fovScale;
uniform float aDistance;

const float PI = 3.14159265359;
const float EPSILON = 0.0001;
const float INFINITY = 1000000.0;
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
///----
/// Simplex 3D Noise
/// by Ian McEwan, Ashima Arts
vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}
vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    //  x0 = x0 - 0. + 0.0 * C
    vec3 x1 = x0 - i1 + 1.0 * C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1. + 3.0 * C.xxx;

    // Permutations
    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(i.z + vec4(0.0, i1.z, i2.z, 1.0)) + i.y +
                    vec4(0.0, i1.y, i2.y, 1.0)) +
                i.x + vec4(0.0, i1.x, i2.x, 1.0));

    // Gradients
    // ( N*N points uniformly over a square, mapped onto an octahedron.)
    float n_ = 1.0 / 7.0; // N=7
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z); //  mod(p,N*N)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_); // mod(j,N)

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    // Normalise gradients
    vec4 norm =
        taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // Mix final noise value
    vec4 m =
        max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 *
        dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
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
    if (p >= aDistance) {
        color += add_color(pos, dir, color, alpha);
        return color;
    }
    float h2 = dot(h, h);
    float to = (length(pos) - aDistance);
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
    vec3 right = normalize(cross(camera_up, camera_dir));
    vec3 finalUp = cross(camera_dir, right);
    vec3 rayDir = normalize(vec3(-uv.x * fovScale, uv.y * fovScale, 1.0));
    vec3 dir = normalize(rayDir.x * right + rayDir.y * finalUp + rayDir.z * camera_dir);
    gl_FragColor = vec4(ray_marching(pos, normalize(dir)), 1.0f);
}
