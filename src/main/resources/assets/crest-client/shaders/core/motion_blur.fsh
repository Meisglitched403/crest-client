#version 330

uniform sampler2D InSampler;

layout(std140) uniform BlitConfig {
    vec4 ColorModulate;
};

in vec2 texCoord;

out vec4 fragColor;

void main(){
    fragColor = vec4(texture(InSampler, texCoord).rgb, ColorModulate.a);
}
