package com.simplerender.plugin.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;

public class GltfDebugger {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: GltfDebugger <path-to-kb>");
            return;
        }
        Path path = Path.of(args[0]);
        System.out.println("Inspecting: " + path);

        if (path.toString().endsWith(".glb") || path.toString().endsWith(".gltf")) {
            System.out.println("Using GltfModelImporter...");
            GltfModelImporter importer = new GltfModelImporter();
            try {
                importer.importModel(path);
                System.out.println("Import successful.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        String json = Files.readString(path);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        printNode(0, root.getAsJsonArray("nodes"), 0);
    }

    private static void printNode(int index, JsonArray nodes, int depth) {
        if (index < 0 || index >= nodes.size())
            return;
        JsonObject node = nodes.get(index).getAsJsonObject();
        String indent = "  ".repeat(depth);
        String name = node.has("name") ? node.get("name").getAsString() : "Node_" + index;
        System.out.println(indent + "+ " + name);
        if (node.has("children")) {
            for (JsonElement c : node.getAsJsonArray("children")) {
                printNode(c.getAsInt(), nodes, depth + 1);
            }
        }
    }
}
