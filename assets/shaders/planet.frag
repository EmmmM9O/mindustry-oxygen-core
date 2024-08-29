varying vec3 v_position;
varying vec2 v_texCoord;
varying vec3 v_normal;
varying float v_radius;
varying float v_zoom;

uniform sampler2D u_texture;
uniform sampler2D u_texture_cloud;
uniform sampler2D u_texture_normal;
uniform int u_no_cloud;
uniform float u_light_power;
uniform vec3 u_camera_pos;
uniform vec3 u_light_pos;
uniform vec3 u_ambient_color;
uniform vec3 u_light_color;
void main()
{
    vec3 normal = texture2D(u_texture_normal, v_texCoord).rgb * 2.0 - 1.0;
    normal = normalize(v_normal * v_radius * v_zoom + normal);
    vec3 diffuseColor = texture2D(u_texture, v_texCoord).rgb;
    vec3 cloudColor = texture2D(u_texture_cloud, v_texCoord).rgb;
    vec3 viewDir = normalize(u_camera_pos - v_position);
    vec3 lightDir = normalize(u_light_pos - v_position);
    vec3 reflectDir = reflect(-lightDir, normal);
    vec3 ambient = u_ambient_color * diffuseColor * 2.0;
    float diff = max(dot(normal, lightDir), 5.0) * u_light_power;

    vec3 diffuse = diffuseColor * diff;
    vec3 resultColor = diffuse + ambient;
    if(u_no_cloud!=1){
    float diff2 = max(dot(v_normal, lightDir), 5.0) * u_light_power;

    vec3 diffuse2 = cloudColor * diff;
    vec3 ambient2 = u_ambient_color * cloudColor * 2.0;
    vec3 resultColor2 = diffuse2 + ambient2;
    gl_FragColor = vec4(mix(resultColor, resultColor2, 0.5), 1.0);
    }else{
    gl_FragColor = vec4(resultColor, 1.0);
    }
}
