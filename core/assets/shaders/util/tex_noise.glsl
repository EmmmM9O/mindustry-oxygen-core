uniform sampler2D noise_tex;
//reference:https://www.shadertoy.com/view/lstSRS
float iqnoise(in vec3 x)
{
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = (p.xy + vec2(37.0, 17.0) * p.z) + f.xy;
    vec2 rg = textureLod(noise_tex, (uv + 0.5) / 256.0, 0.0).yx;
    return -1.0 + 2.0 * mix(rg.x, rg.y, f.z);
}
