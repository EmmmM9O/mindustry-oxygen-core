#ifndef ZBATCH_GLSL
#define ZBATCH_GLSL
#include "/utils/shadow.glsl"

const float shadowIntensity = 0.6;
const float ambientIntensity = 0.2;
vec3 lightColor = vec3(1.0);

#define BATCHRENDERER                                                                                                  \
    gl_FragColor =                                                                                                     \
        vec4(color.rgb * lightColor *                                                                                  \
                 (ambientIntensity + (1.0 - shadow * shadowIntensity) * (dot(normal, -u_lightDir) * 0.5 + 0.5) *       \
                                         (1.0 - ambientIntensity)),                                                    \
             color.a);

#endif // ZBATCH_GLSL
