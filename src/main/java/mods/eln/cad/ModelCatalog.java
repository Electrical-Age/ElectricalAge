package mods.eln.cad;

import org.erisia.cad.Model;
import org.erisia.cad.obj.ObjWriter;
import org.erisia.cad.tex.Atlas;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * The static models made with the checked model library (board #44): each is written by ./gradlew cadModels into the
 * mod's resources (committed, as datagen output is), and ModelCatalogTest checks the committed files are what the
 * catalog writes and that every model passes the checker. Models come here from tools/models (the Python code-CAD)
 * one at a time, when they are next changed; one whose look would change waits for Baughn's word.
 */
public final class ModelCatalog {
    private ModelCatalog() {
    }

    /** A block model (neoforge:obj): assets/eln/models/block/NAME.{obj,mtl}; the model JSON stays hand-written. */
    public record Block(String name, Supplier<Model> model) {
    }

    /** An EA node model (Obj3D): assets/eln/model/NAME/NAME.{obj,mtl} and its atlas tex.png. */
    public record Node(String name, Supplier<List<ObjWriter.EPart>> parts, Atlas atlas, long seed) {
    }

    public static final List<Block> BLOCKS = List.of();
    public static final List<Node> NODES = List.of();

    /** Write every model under resources (the mod's src/main/resources). */
    public static void writeAll(Path resources) throws java.io.IOException {
        Path blocks = resources.resolve("assets/eln/models/block");
        for (Block b : BLOCKS) ObjWriter.block(b.model().get(), blocks, b.name());
        for (Node n : NODES) {
            Path dir = resources.resolve("assets/eln/model/" + n.name());
            Files.createDirectories(dir);
            ObjWriter.eln(n.parts().get(), n.atlas(), dir, n.name(), "tex.png");
            n.atlas().paint(n.seed()).writePng(dir.resolve("tex.png"));
        }
    }

    /** Usage: ModelCatalog RESOURCES_DIR. */
    public static void main(String[] args) throws java.io.IOException {
        writeAll(Path.of(args[0]));
        System.out.println("cadModels: " + BLOCKS.size() + " block models, " + NODES.size() + " node models written");
    }
}
