#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

uniform float Saturation;

out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, texCoord);

    const vec3 Gray = vec3(0.3, 0.59, 0.11);
    float luma = dot(color.rgb, Gray);
    vec3 chroma = color.rgb - luma;
    vec3 outColor = chroma * Saturation + luma;

    fragColor = vec4(outColor, color.a);
}
