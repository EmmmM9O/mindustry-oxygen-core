#ifndef LUMINANCE_GLSL
#define LUMINANCE_GLSL
const vec3 luminanceVector = vec3(0.2126, 0.7152, 0.0722);

float luminance(vec3 color) { return dot(color, luminanceVector); }

#endif // LUMINANCE_GLSL
