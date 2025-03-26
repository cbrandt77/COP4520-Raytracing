package edu.ucf.cop4520raytracing.core;

import edu.ucf.cop4520raytracing.core.light.Light;
import edu.ucf.cop4520raytracing.core.solid.Solid;
import edu.ucf.cop4520raytracing.core.util.ArrayUtil;
import edu.ucf.cop4520raytracing.core.util.Ray3d;
import it.unimi.dsi.fastutil.Pair;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Builder
@Data
public class Scene {
	
	/**
	 * The camera
	 */
	@NonNull @Builder.Default private final Camera camera = Camera.builder().build();
	/**
	 * The solids
	 */
	@NonNull private final Solid[] solids;
	/**
	 * The lights
	 */
	@NonNull private final Light[] lights;
	/**
	 * The skybox generator-- (Direction) -> Color
	 */
	@NonNull private final Function<Vector3dc, Color> skyboxGenerator;
	/**
	 * The render executor. This is managed and automatically closed
	 */
	@NonNull private final ExecutorService renderExecutor;
	/**
	 * The image
	 */
	@NonNull @Builder.Default private final BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
	
	public void render() {
		final int height = image.getHeight();
		final int width = image.getWidth();
		
		var stream = getCoordProducts(width, height);

		stream.parallel()
		      .map(coord -> Pair.of(coord, normalizeCoordinate(coord, width, height)))
		      .map(coord_vec -> Pair.of(coord_vec.left(), new Ray3d(camera.getPosition(), coord_vec.right())))
		      .forEach(this::castRay);
		/*
		for (int y = 0; y < height; y++) {
			int finalY = y;
			renderExecutor.execute(() -> {
				for (int x = 0; x < width; x++) {
					// Convert the pixel (screen) coordinates to world coordinates
					double xNorm = (x - width / 2.0) / width;
					double yNorm = (finalY - height / 2.0) / height;
					Vector3d dir = new Vector3d(xNorm, yNorm, -1).normalize();
					dir.rotateY(Math.toRadians(camera.getYaw()));
					dir.rotateX(Math.toRadians(camera.getPitch()));

					// Compute the ray
					Ray3d ray = new Ray3d(camera.getPosition(), dir);
					// Compute the color of the skybox using this ray
					Color pixelColor = skyboxGenerator.apply(ray.direction());
					// Compute intersection & hit context
					double closestT = Double.MAX_VALUE;
					Vector3dc hitNormal = null;
					Vector3dc hitPosition = null;
					Solid hitSolid = null;

					for (Solid solid : solids) {
						double t = solid.intersect(ray);
						if (t != Solid.NO_HIT && t < closestT) {
							closestT = t;
							hitSolid = solid;
							hitPosition = new Vector3d(ray.origin()).add(new Vector3d(ray.direction()).mul(t));
							hitNormal = solid.getNormal(ray, t);
							pixelColor = solid.getColor();
						}
					}

					// If we hit something, apply lighting to it
					if (hitSolid != null) {
						for (Light light : lights) {
							pixelColor = light.applyLighting(pixelColor, hitPosition, hitNormal);
						}
					}

					// & set this data on the image
					image.setRGB(x, finalY, pixelColor.getRGB());
				}
			});
		}*/
	}
	
	// this works well with the JVM's runtime optimizer
	private Stream<Coordinate> getCoordProducts(int width, int height) {
		return IntStream.range(0, height).parallel()
		                .mapToObj(y -> IntStream.range(0, width).parallel().mapToObj(x -> new Coordinate(x, y)))
		                .flatMap(Function.identity());
	}
	
	private Vector3d normalizeCoordinate(Coordinate xy, int width, int height) {
		double xNorm = (xy.x() - width / 2.0) / width;
		double yNorm = (xy.y() - height / 2.0) / height;
		Vector3d dir = new Vector3d(xNorm, yNorm, -1).normalize();
		dir.rotateY(Math.toRadians(camera.getYaw()));
		dir.rotateX(Math.toRadians(camera.getPitch()));
		return dir;
	}
	
	private void castRay(Pair<Coordinate, Ray3d> coord_ray) {
		var coord = coord_ray.left();
		var ray = coord_ray.right();
		// Compute the color of the skybox using this ray
		Color pixelColor = skyboxGenerator.apply(ray.direction());
		// Compute intersection & hit context
		double closestT = Double.MAX_VALUE;
		Vector3dc hitNormal = null;
		Vector3dc hitPosition = null;
		Solid hitSolid = null;
		
		for (Solid solid : solids) {
			double t = solid.intersect(ray);
			if (t != Solid.NO_HIT && t < closestT) {
				closestT = t;
				hitSolid = solid;
				hitPosition = new Vector3d(ray.origin()).add(new Vector3d(ray.direction()).mul(t));
				hitNormal = solid.getNormal(ray, t);
				pixelColor = solid.getColor();
			}
		}
		
		// If we hit something, apply lighting to it
		if (hitSolid != null) {
			for (Light light : lights) {
				pixelColor = light.applyLighting(pixelColor, hitPosition, hitNormal);
			}
		}
		
		// & set this data on the image
		image.setRGB(coord.x(), coord.y(), pixelColor.getRGB());
	}
	
	private record Coordinate(int x, int y) {}
	
	public static class SceneBuilder {
		/**
		 * See {@link Scene#solids}
		 */
		public SceneBuilder solids(Solid... solids) {
			this.solids = ArrayUtil.concat(this.solids, solids);
			return this;
		}
		
		/**
		 * See {@link Scene#lights}
		 */
		public SceneBuilder lights(Light... lights) {
			this.lights = ArrayUtil.concat(this.lights, lights);
			return this;
		}
	}
}
