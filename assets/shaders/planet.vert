@import("transpose");
@import("inverse");
attribute vec4 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord;
varying float v_radius;
varying float v_zoom;


uniform mat4 u_projection;
uniform mat4 u_model;
uniform mat4 u_view;
uniform int u_no_cloud;
uniform float u_zoom;
uniform float u_radius;
uniform float refractionIndex;
uniform float refractionPower;

void main() {
    v_texCoord = a_texCoord0;
    v_radius = u_radius;
    v_zoom = u_zoom;
    vec4 pos = a_position;
    pos.x *= u_radius;
    pos.y *= u_radius;
    pos.z *= u_radius;
    v_position = vec3(u_model * pos);
    if(u_no_cloud!=1){
	    /*
    vec3 viewDir = normalize(vec3(u_view * vec4(v_position, 1.0)) - vec3(u_view * vec4(0.0, 0.0, 0.0, 1.0)));
    vec3 refraction = normalize(refract(viewDir, normalize(a_normal), refractionIndex));
    pos.xyz += refraction * refractionPower;
    v_position = vec3(u_model * pos);*/
	    //TODD
    }
    v_normal = normalize(mat3(transpose(inverse(u_model))) * a_normal);
    gl_Position = u_projection * u_view * vec4(v_position, 1.0f);
}
