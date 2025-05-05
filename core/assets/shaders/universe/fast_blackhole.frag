#define HIGHP
varying vec2 uv;

uniform vec2 resolution;
uniform vec3 camera_pos;
uniform mat3 camera_mat;
uniform samplerCube galaxy;
uniform sampler2D ray1;
uniform sampler2D ray2;
uniform sampler2D ray3;
uniform sampler2D ray4;
uniform sampler2D ray5;
uniform sampler2D ray6;
uniform sampler2D ray7;
uniform sampler2D ray8;
uniform float time;
uniform float fov_scale;
uniform float start_distance;

vec4 get_color(vec3 pos, vec3 dir) {
    vec3 h = cross(pos, dir);
    float h2 = dot(h, h);
    float dis = sqrt(h2);
    if (dis >= start_distance - 0.1) {
        return texture(galaxy, dir);
    }

    float pos2 = min(dot(pos, pos), start_distance * start_distance);
    float from = sqrt(pos2 - h2);
    float mf = sqrt(start_distance * start_distance - h2);
    float hu = min(from / mf, 1.0);
    float wu = min(dis / start_distance, 1.0);
    vec2 uv = vec2(wu, hu);
    vec4 coord = texture(ray1, uv);
    if (coord.x <= 0.0001 && coord.y <= 0.0001) {
        return vec4(0.0, 0.0, 0.0, 1.0);
    }
    vec3 u = -h / dis;
    float sinv = (coord.y * 2.0 - 1.0);
    float cosv = (coord.x * 2.0 - 1.0);
    dir = dir * cosv + cross(u, dir) * sinv + u * dot(u, dir) * (1.0 - cosv);

    return texture(galaxy, dir);
}
void main() {
    vec3 pos = camera_pos;
    vec2 uvp = (gl_FragCoord.xy) / resolution - vec2(0.5);
    uvp.x *= resolution.x / resolution.y;
    vec3 rayDir = normalize(vec3(-uvp.x * fov_scale, uvp.y * fov_scale, 1.0));
    vec3 dir = rayDir * camera_mat;
    gl_FragColor = get_color(pos, normalize(dir));
}
