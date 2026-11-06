#version 330
#extension GL_ARB_separate_shader_objects : require

// Electrical Age: lamp beams (flashlights, the headlamp, the bike lamp), drawn per pixel so they follow the view with no lag (Baughn).
// Data in a 128x1 texture of packed floats (LampBeam.java): [0..15] inverse(projection * view rotation);
// [16] beam count; [17] strength; [18..20] tint; [21] near cut (m, faded in over 0.12: the hand's depth is merged into the main buffer);
// [24 + 9i ..] beam i: origin - camera (3), direction (3), length (blocks: the falloff), cos of the cone's half
// angle, cut (blocks: where the beam ends, just past the first block it hits; = length if nothing).
// The scene's albedo isn't known here, so the beam multiplies what is there, guarded by luminance: dark surfaces
// brighten a lot, lit ones hardly change.

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
uniform sampler2D DataSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float data(int i) {
    uvec4 b = uvec4(round(texelFetch(DataSampler, ivec2(i, 0), 0) * 255.0));
    return uintBitsToFloat(b.r | (b.g << 8) | (b.b << 16) | (b.a << 24));
}

// world position minus the camera of the pixel at uv (from the depth buffer)
vec3 worldAt(mat4 inv, vec2 uv) {
    vec4 h = inv * vec4(uv * 2.0 - 1.0, texture(DepthSampler, uv).r, 1.0);
    return h.xyz / h.w;
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    float depth = texture(DepthSampler, texCoord).r;
    int n = int(data(16));
    // 26.3: reversed Z, the sky is depth 0
    if (n == 0 || depth <= 0.0) {
        fragColor = vec4(c, 1.0);
        return;
    }
    mat4 inv = mat4(data(0), data(1), data(2), data(3), data(4), data(5), data(6), data(7),
                    data(8), data(9), data(10), data(11), data(12), data(13), data(14), data(15));
    vec4 h = inv * vec4(texCoord * 2.0 - 1.0, depth, 1.0);
    vec3 p = h.xyz / h.w;                                   // world position minus the camera
    // the near cut (the hand is merged into the depth buffer): faded in over 0.12 m, so a wall right in front isn't
    // cut out in a hard circle
    float nearFade = smoothstep(data(21), data(21) + 0.12, length(p));
    if (nearFade <= 0.0) {
        fragColor = vec4(c, 1.0);
        return;
    }
    // the surface normal from the neighbouring pixels' positions, each axis from the side whose depth is closer (Baughn:
    // dFdx/dFdy mixed two planes at every corner, a bright or dark line along each, plain in a 1-wide corridor)
    // (the side that continues in a straight line: two pixels out, the smaller bend)
    vec2 px = 1.0 / InSize;
    vec3 pr = worldAt(inv, texCoord + vec2(px.x, 0.0)), pl = worldAt(inv, texCoord - vec2(px.x, 0.0));
    vec3 pr2 = worldAt(inv, texCoord + vec2(2.0 * px.x, 0.0)), pl2 = worldAt(inv, texCoord - vec2(2.0 * px.x, 0.0));
    vec3 pu = worldAt(inv, texCoord + vec2(0.0, px.y)), pd = worldAt(inv, texCoord - vec2(0.0, px.y));
    vec3 pu2 = worldAt(inv, texCoord + vec2(0.0, 2.0 * px.y)), pd2 = worldAt(inv, texCoord - vec2(0.0, 2.0 * px.y));
    vec3 dx = length(pr2 - 2.0 * pr + p) < length(p - 2.0 * pl + pl2) ? pr - p : p - pl;
    vec3 dy = length(pu2 - 2.0 * pu + p) < length(p - 2.0 * pd + pd2) ? pu - p : p - pd;
    vec3 nrm = cross(dx, dy);
    float nl = length(nrm);
    nrm = nl > 1e-6 ? nrm / nl : -normalize(p);
    if (dot(nrm, p) > 0.0) nrm = -nrm;
    float light = 0.0;
    for (int i = 0; i < n && i < 5; i++) {
        int o = 24 + 9 * i;
        vec3 org = vec3(data(o), data(o + 1), data(o + 2));
        vec3 dir = vec3(data(o + 3), data(o + 4), data(o + 5));
        float len = data(o + 6), cosHalf = data(o + 7), cut = data(o + 8);
        vec3 l = p - org;
        float d = length(l);
        if (d > min(len, cut) || d < 1e-3) continue;
        vec3 ld = l / d;
        float cone = smoothstep(cosHalf, mix(cosHalf, 1.0, 0.6), dot(ld, dir));
        float fall = (1.0 - smoothstep(0.6 * len, len, d)) / (1.0 + 0.012 * d * d);
        // facing the beam: Lambert, with some wrap so grazing walls still catch light
        float lambert = clamp(dot(nrm, -ld) * 0.75 + 0.25, 0.0, 1.0);
        light += cone * fall * lambert;
    }
    float lum = dot(c, vec3(0.299, 0.587, 0.114));
    float gain = data(17) * light * nearFade * (1.0 - smoothstep(0.05, 0.6, lum));
    vec3 tint = vec3(data(18), data(19), data(20));
    // the added light with a soft shoulder (about the plain sum while dim, never past white), so a close wall isn't
    // washed out (AdventurAgent); pixels outside the beam are unchanged
    vec3 add = c * gain * tint, room = max(1.0 - c, vec3(1e-3));
    fragColor = vec4(c + room * (1.0 - exp(-add / room)), 1.0);
}
