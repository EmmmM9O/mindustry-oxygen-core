#import(util/tex_noise.glsl);
#import(util/math.glsl);
#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform mat3 camera_mat;
uniform samplerCube galaxy;
uniform sampler2D color_map;
uniform sampler2D previous_tex;
uniform float time;

uniform float max_length_2;
uniform float horizon_radius_2;
uniform float adisk_inner_radius;
uniform float adisk_outer_radius;
uniform float adisk_noise_scale;
uniform float adisk_noise_LOD_1;
uniform float adisk_noise_LOD_2;
uniform float adisk_speed;
uniform float adisk_lit;
uniform float max_scl;
uniform float min_scl;
uniform float scl_r;
uniform float scl_t;
uniform float max_steps;
uniform float step_size;
uniform float fov_scale;
uniform float a_distance;
uniform float has_previous;

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
float sdTorus(vec3 p, vec2 t)
{
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}
///---- from https://github.com/rossning92/Blackhole
/// and https://www.shadertoy.com/view/lstSRS
void adisk_color(vec3 pos, inout vec3 color, inout float alpha, float scl) { //吸积盘
    //密度
    vec3 sphericalCoord = toSpherical(pos);

    float distF = sphericalCoord.x;
    float distFD = sphericalCoord.z;
    float radialGradient = 1.0 - saturate((distF - adisk_inner_radius) / (adisk_outer_radius - adisk_inner_radius) * 0.5);

    float coverage = pcurve(radialGradient, 4.0, 0.9);
    float discThickness = 0.1;
    discThickness *= radialGradient;
    coverage *= saturate(1.0 - abs(distFD) / discThickness);
    float fade = pow((abs(distF - adisk_inner_radius) + 0.4), 4.0) * 0.04;
    float bloomFactor = 1.0 / (pow(distFD, 2.0) * 40.0 + fade + 0.00002);
    coverage = saturate(coverage * 0.7);
    coverage = saturate(coverage + bloomFactor * bloomFactor * 0.2);
    if (coverage < 0.01)
    {
        return;
    }
    vec3 dustColorLit = vec3(1.0);
    vec3 dustColorDark = vec3(0.0, 0.0, 0.0);
    float dustGlow = 1.0 / (pow(1.0 - radialGradient, 2.0) * 290.0 + 0.002);
    vec3 dustColor = dustColorLit * dustGlow * 8.2;
    vec3 b = dustColorLit * pow(bloomFactor, 1.5);
    b *= mix(vec3(1.7, 1.1, 1.0), vec3(0.5, 0.6, 1.0), vec3(pow(radialGradient, 2.0)));
    b *= mix(vec3(1.7, 0.5, 0.1), vec3(1.0), vec3(pow(radialGradient, 0.5)));

    dustColor = mix(dustColor, b * 150.0, saturate(1.0 - coverage * 1.0));
    sphericalCoord.y *= 2.0;
    sphericalCoord.z *= 4.0;
    float noise = 1.0;
    vec3 rc = sphericalCoord;
    float sta = adisk_noise_scale;
    for (int i = 0; i < int(adisk_noise_LOD_1); i++) {
        noise *= 0.5 * snoise((rc * sta) * float(i * i)) + 0.5;
        sta *= 2.0;
        if (i % 2 == 0) {
            rc.y += time * adisk_speed;
        } else {
            rc.y -= time * adisk_speed;
        }
    }
    rc = sphericalCoord + 30.0;
    float noise_2 = 2.0;
    sta = adisk_noise_scale;
    for (int i = 0; i < int(adisk_noise_LOD_2); i++) {
        noise_2 *= 0.5 * snoise((rc * sta) * float(i * i)) + 0.5;
        sta *= 2.0;
        if (i % 2 == 0) {
            rc.y += time * adisk_speed;
        } else {
            rc.y -= time * adisk_speed;
        }
    }
    coverage *= noise_2;
    coverage = saturate(coverage * 6.0);
    coverage *= pcurve(radialGradient, 4.0, 0.9);
    coverage *= 0.2;
    sphericalCoord.y += time * adisk_speed * 0.5;
    dustColor *= noise * 0.998 + 0.002;
    dustColor *= pow(texture(color_map, sphericalCoord.yx * vec2(0.15, 0.27)).rgb, vec3(2.0)) * 4.0;

    color += adisk_lit * dustColor * (1.0 - alpha) * coverage;
    alpha = (1.0 - alpha) * coverage + alpha;
    alpha = min(alpha, 1.0);
    vec2 t = vec2(1.0, 0.01);

    float torusDist = length(sdTorus(pos + vec3(0.0, -0.05, 0.0), t));

    float bloomDisc = 1.0 / (pow(torusDist, 2.0) + 0.001);
    vec3 col = vec3(1.0);
    bloomDisc *= length(pos) < 0.5 ? 0.0 : 1.0;

    color += col * bloomDisc * (2.9 / 600.0) * (1.0 - alpha);
}
vec3 add_color(vec3 pos, vec3 dir, vec3 color, float alpha);
vec3 ray_marching(vec3 pos, vec3 dir) {
    vec3 color = vec3(0.0);
    float alpha = 0.0;

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
        if (len >= max_length_2)
            break;
        float scl = mix(min_scl, max_scl, 1.0 - smoothstep(0.0f, scl_t, inversesqrt(len) / scl_r));

        dir += accel(h2, pos) * scl; //引力透镜
        if (len <= horizon_radius_2) {
            //到达事件视界
            return color;
        }
        adisk_color(pos, color, alpha, scl); //吸积盘
        pos += dir * scl;
    }
    color += add_color(pos, dir, color, 1.0 - alpha);
    return color;
}

void main() {
    vec3 pos = camera_pos;
    vec2 jitter = vec2(
            snoise(vec3(gl_FragCoord.xy, time)),
            snoise(vec3(gl_FragCoord.yx, time + 100.0))
        ) * 0.005 / resolution;
    vec2 uvp = (gl_FragCoord.xy + jitter) / resolution - vec2(0.5);
    uvp.x *= resolution.x / resolution.y;
    vec3 rayDir = normalize(vec3(-uvp.x * fov_scale, uvp.y * fov_scale, 1.0));
    vec3 dir = rayDir * camera_mat;
    vec3 color = ray_marching(pos, normalize(dir)) * 1.0;
    if (has_previous >= 0.5) {
        const float p = 1.0;
        vec3 previous = pow(texture(previous_tex, uv).rgb, vec3(1.0 / p));

        color = pow(color, vec3(1.0 / p));

        float blendWeight = 0.1;

        color = mix(color, previous, blendWeight);

        color = pow(color, vec3(p));
    }
    gl_FragColor = vec4(color, 1.0);
}
