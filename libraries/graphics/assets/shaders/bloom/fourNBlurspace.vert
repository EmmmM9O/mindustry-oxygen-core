attribute vec4 a_position;
attribute vec2 a_texCoord0;

uniform vec2 u_resolution;

varying vec2 v_texCoords0;
varying vec2 v_texCoords1;
varying vec2 v_texCoords2;
varying vec2 v_texCoords3;
varying vec2 v_texCoords4;

void main() {
    vec2 size = 1.0 / u_resolution;
    vec4 o = size.xyxy * vec4(-1.0, -1.0, 1.0, 1.0);

    v_texCoords0 = a_texCoord0 + o.xy; // -1 -1
    v_texCoords1 = a_texCoord0 + o.zy; // 1 -1
    v_texCoords2 = a_texCoord0;
    v_texCoords3 = a_texCoord0 + o.xw; // -1 1
    v_texCoords4 = a_texCoord0 + o.zw; // 1 1

    gl_Position = a_position;
}
