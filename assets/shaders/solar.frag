varying vec3 v_position;
varying vec3 v_normal;
varying vec2 v_texCoord;

uniform sampler2D u_texture;
uniform vec3 u_camera_pos;
uniform float u_haloIntensity;
uniform float u_zoom;

void main() {
    vec4 textureColor = texture2D(u_texture, v_texCoord);
    float distance = length(u_camera_pos - v_position) * u_zoom;
    float halo = exp(-distance * u_haloIntensity);
    float ambient = 0.1f;
    vec3 lightDir = normalize(u_camera_pos - v_position);
    float diff = max(dot(v_normal, lightDir), 0.0)*1.5f;
    vec4 finalColor = textureColor * (ambient + diff) * halo;
    gl_FragColor = finalColor;
}
