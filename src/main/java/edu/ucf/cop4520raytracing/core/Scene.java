package edu.ucf.cop4520raytracing.core;

import edu.ucf.cop4520raytracing.core.light.DirectionalLight;
import edu.ucf.cop4520raytracing.core.light.Light;
import edu.ucf.cop4520raytracing.core.solid.Plane;
import edu.ucf.cop4520raytracing.core.solid.Solid;
import edu.ucf.cop4520raytracing.core.solid.Sphere;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * A capsule of solids, lights, and a skybox generator, for use in rendering by Raytracer.
 */
@Data @NoArgsConstructor
public class Scene implements Cloneable {
    public static final Scene DEFAULT = new Scene().setSkybox(dir -> new Color(182, 227, 229))
                                                   .addSolids(
                                                           new Plane(new Vector3d(0, -5, 0), new Vector3d(0, 1, 0), Color.GRAY),
                                                           new Sphere(new Vector3d(0, 0, -5), 1, Color.RED),
                                                           new Sphere(new Vector3d(-2, 0, -4), 0.4, Color.GREEN),
                                                           new Sphere(new Vector3d(2, -2, -6), 2.2, Color.CYAN)
                                                   )
                                                   .addLights(
                                                           new DirectionalLight(new Vector3d(0, 2, 0), Color.WHITE, 0.02)
                                                   );

    /**
     * The solids
     */
    private final List<Solid> solids = new ArrayList<>();
    /**
     * The lights
     */
    private final List<Light> lights = new ArrayList<>();
    /**
     * The skybox generator-- (Direction) -> Color
     */
    private Function<Vector3dc, Color> skyboxGenerator = it -> Color.black;
    /**
     * The render executor. This is managed and automatically closed.
     */
    private final ExecutorService renderExecutor = Executors.newSingleThreadScheduledExecutor();
    


    public Scene addLights(Light... lights) {
        Collections.addAll(this.lights, lights);
        return this;
    }

    public Scene removeLights(Light... lights) {
        for (Light light : lights) {
            this.lights.remove(light);
        }
        return this;
    }

    public Scene addSolids(Solid... solids) {
        Collections.addAll(this.solids, solids);
        return this;
    }

    public Scene removeSolids(Solid... solids) {
        for (Solid solid : solids) {
            this.solids.remove(solid);
        }
        return this;
    }

    public Scene setSkybox(Function<Vector3dc, Color> skyboxGenerator) {
        this.skyboxGenerator = skyboxGenerator;
        return this;
    }

    @Override
    public Scene clone() {
        try {
            Scene clone = (Scene) super.clone();
            clone.setSkybox(this.skyboxGenerator)
                 .addSolids(this.solids.toArray(Solid[]::new))
                 .addLights(this.lights.toArray(Light[]::new));
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

