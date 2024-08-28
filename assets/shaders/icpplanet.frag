uniform sampler2D u_texture;

varying vec2 v_texCoords;

void main(){
  gl_FragColor = texture2D(u_texture,v_texCoords)*vec4(1.0f, 1.0f, 1.0f, 1.0f);
}
