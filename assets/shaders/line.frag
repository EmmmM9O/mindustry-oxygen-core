varying vec3 v_position;
varying vec2 v_texCoord;
varying vec3 v_normal;
varying float v_radius;
varying float v_zoom;

uniform float u_light_power;
uniform vec3 u_camera_pos;
uniform vec3 u_light_pos;
uniform vec3 u_ambient_color;
uniform vec3 u_light_color;
uniform vec4 color;
void main()
{
    vec3 normal = normalize(v_normal);

    vec3 diffuseColor = color.rgb;
    vec3 viewDir = normalize(u_camera_pos - v_position);
    vec3 lightDir = normalize(u_light_pos - v_position);
    vec3 reflectDir = reflect(-lightDir, normal);
    vec3 ambient = u_ambient_color * diffuseColor * 2.0;
    float diff = max(dot(normal, lightDir), 3.0) * u_light_power;

    vec3 diffuse = diffuseColor * diff;
    vec3 resultColor = diffuse + ambient;
    gl_FragColor = vec4(resultColor, color);
}
