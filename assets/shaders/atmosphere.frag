varying vec3 v_position;
varying vec2 v_texCoord;
varying vec3 v_normal;

uniform sampler2D u_texture;
uniform sampler2D u_texture_normal;
uniform vec3 u_camera_pos;
uniform vec3 u_light_pos;
uniform vec3 u_ambient_color;
uniform vec3 u_light_color;
void main()
{
    vec4 color = texture2D(u_texture, v_texCoord);
    vec3 diffuseColor = color.rgb;
    vec3 viewDir = normalize(u_camera_pos - v_position);
    vec3 lightDir = normalize(u_light_pos - v_position);
    vec3 reflectDir = reflect(-lightDir, v_normal);
    vec3 ambient = u_ambient_color * diffuseColor * 2.0;
    float diff = max(dot(v_normal, lightDir), 4.0) * 0.003;

    vec3 diffuse = diffuseColor * diff;
    vec3 resultColor = diffuse + ambient;
    gl_FragColor = vec4(resultColor, color.a);
}
