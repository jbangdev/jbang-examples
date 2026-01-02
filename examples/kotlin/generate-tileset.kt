///usr/bin/env jbang "$0" "$@" ; exit $?
//KOTLIN 2.3.0
//DEPS org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0

import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

// -------- config --------
/** Directory where the input PNG images are located. */
const val INPUT_DIR = "./"

/** Filename of the generated tileset atlas image. */
const val OUTPUT_IMAGE = "tileset.png"

/** Filename of the generated Tiled-compatible JSON metadata file. */
const val OUTPUT_META = "tileset.json"

/** The fixed width of each tile in pixels. */
const val TILE_WIDTH = 96

/** The fixed height of each tile in pixels. */
const val TILE_HEIGHT = 96

/** The spacing (padding) between tiles in the atlas, in pixels. */
const val PADDING = 2

/** The number of tiles to arrange in each row of the atlas. */
const val COLUMNS = 8   // tiles per row
// ------------------------

/**
 * This is a JBang script to generate a tileset from a directory of images.
 * It combines multiple PNG files into a single atlas image and generates
 * a corresponding Tiled-compatible JSON metadata file.
 *
 * Configuration is handled via constants at the top of the file.
 * The script performs the following actions:
 * 1. Scans the input directory for PNG files (excluding the output image itself, and files starting with "." or "_").
 * 2. Skips any unreadable or corrupted images.
 * 3. Arranges tiles into rows and columns with optional padding.
 * 4. Generates a JSON tileset metadata file in Tiled JSON format (.tsj).
 *
 * @see <a href="https://doc.mapeditor.org/en/stable/reference/json-map-format/#tileset">Tiled JSON Map Format - Tileset</a>
 */
fun main() {
    val inputDir = Path(INPUT_DIR)
    if (!inputDir.exists() || !inputDir.isDirectory()) {
        println("Input directory $INPUT_DIR does not exist or is not a directory.")
        System.exit(-1)
    }

    val files = inputDir.listDirectoryEntries("*.png")
        .filter { it.isRegularFile() && it.name != OUTPUT_IMAGE && !it.name.startsWith(".") && !it.name.startsWith("_") }
        .sortedBy { it.name }

    if (files.isEmpty()) {
        println("No PNG files found in $INPUT_DIR")
        System.exit(0)
    }

    println("Found ${files.size} PNG files. Generating tileset...")

    val rows = (files.size + COLUMNS - 1) / COLUMNS

    val atlasWidth = COLUMNS * TILE_WIDTH + (COLUMNS - 1) * PADDING
    val atlasHeight = rows * TILE_HEIGHT + (rows - 1) * PADDING

    val atlas = BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = atlas.createGraphics()
    graphics.setRenderingHint(
        java.awt.RenderingHints.KEY_INTERPOLATION,
        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
    )

    val tilesMetadata = mutableListOf<JsonElement>()

    var processedCount = 0
    files.forEachIndexed { index, path ->
        val x = processedCount % COLUMNS
        val y = processedCount / COLUMNS

        val px = x * (TILE_WIDTH + PADDING)
        val py = y * (TILE_HEIGHT + PADDING)

        val tile = try {
            ImageIO.read(path.toFile())
        } catch (e: Exception) {
            null
        } ?: run {
            println("Skipping unreadable image: ${path.name}")
            return@forEachIndexed
        }
        graphics.drawImage(tile, px, py, TILE_WIDTH, TILE_HEIGHT, null)

        tilesMetadata.add(buildJsonObject {
            put("id", processedCount)
            put("properties", buildJsonArray {
                add(buildJsonObject {
                    put("name", "name")
                    put("type", "string")
                    put("value", path.name.substringBeforeLast('.'))
                })
                add(buildJsonObject {
                    put("name", "filename")
                    put("type", "string")
                    put("value", path.name)
                })
            })
        })
        processedCount++
        if (processedCount % 10 == 0 || processedCount == files.size) {
            print("\rProcessed $processedCount/${files.size} images...")
        }
    }
    println("\rProcessed $processedCount images. Finished processing.")

    graphics.dispose()

    val outputImageFile = Path(OUTPUT_IMAGE)
    ImageIO.write(atlas, "png", outputImageFile.toFile())

    val metadata = buildJsonObject {
        put("columns", COLUMNS)
        put("image", OUTPUT_IMAGE)
        put("imageheight", atlasHeight)
        put("imagewidth", atlasWidth)
        put("margin", 0)
        put("name", OUTPUT_IMAGE.substringBeforeLast("."))
        put("objectalignment", "unspecified")
        put("spacing", PADDING)
        put("tilecount", tilesMetadata.size)
        put("tileheight", TILE_HEIGHT)
        put("tilewidth", TILE_WIDTH)
        put("tiles", JsonArray(tilesMetadata))
        put("type", "tileset")
        put("version", "1.10")
        put("tiledversion", "1.11.0")
    }

    val json = Json { prettyPrint = true }
    val outputMetaFile = Path(OUTPUT_META)
    outputMetaFile.writeText(json.encodeToString(metadata))

    println("Created ${outputImageFile.toAbsolutePath()} and ${outputMetaFile.toAbsolutePath()}")
}
