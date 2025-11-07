shader_type spatial;

render_mode cull_back, diffuse_lambert, specular_disabled;

uniform vec3 base_color : source_color = vec3(0.5, 0.7, 0.4);
uniform float roughness : hint_range(0.0, 1.0) = 1.0;
uniform vec3 light_tint : source_color = vec3(1.0, 1.0, 1.0);

void fragment() {
	vec3 flat_normal = normalize(cross(dFdx(VERTEX), dFdy(VERTEX)));
	NORMAL = flat_normal;
	ALBEDO = base_color * light_tint;
	ROUGHNESS = roughness;
}
