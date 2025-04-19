@ import(util/noise.glsl);
#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform mat3 camera_mat;
uniform samplerCube galaxy;
uniform sampler2D color_map;
uniform sampler2D ray;
uniform float time;
uniform float adisk_noise_LOD;
uniform float adisk_speed;
uniform float adisk_lit;
uniform float adisk_particle;
const float minf = 1e-4;
vec3 add_color(vec3 dir);
void main() {
    vec4 value = texture(ray, uv);
    vev3 dir = value * 2.0 - 1.0;
    if (value.w <= minf) {
        if (value.x <= minf && value.y <= minf && value.z <= minf) {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
        }
        gl_FragColor = add_color(dir);
        return;
    }
    vec3 color = vec3(0.0);
    float density = value.w * 100.0;
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
    color += add_color(dir);
    gl_FragColor = vec4(color, 1.0);
}
